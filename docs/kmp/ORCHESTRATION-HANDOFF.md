# StreamCoreTV Web Orchestration Handoff

> Historical WEB-01/02 checkpoint. For the current branch state, completed UI work, new-PC setup, and next actions, read
> [the current handoff](../HANDOFF.md). The model selection, branch status, and pending-ticket instructions below describe that earlier phase.

Use **GPT-5.6 Sol with Ultra reasoning**. Act as orchestration/integration owner, not primary feature implementer. The target architecture is backend-agnostic: core, domain, and feature UI never depend on provider DTOs, SDKs, responses, or models.

## Verified repository state

- `codex/kmp-migration` is at `2267306e5df827deded96addd91b6ec947e9f655`, the accepted WEB-01 merge. Its worktree has a pre-existing untracked `.kotlin/`; preserve it as user-owned.
- `codex/web-02-login-profiles` is clean at `8a391953ae3a77074b1d97428de32cba14a1c760`, nine commits ahead. Production is `28d8fbfdfa26efb386a1b8eb540c89ea4a093a71`; HEAD records live evidence.
- WEB-02's final authorized live run passed Login → Profiles → hard-reload restoration and deleted the temporary TMDB session in `finally`, without exposing credential, session, or account data.
- WEB-02 is **not merged**. No independent review/PR acceptance is recorded in Git; treat read-only review as pending rather than claiming it happened.
- Do not start WEB-03 implementation until the execution protocol and WEB-03/WEB-04 ticket split below are written into the backlog and committed.

## Orchestrator contract

The root owns dependencies, immutable bases, worktrees, path reservations, integration, merge order, evidence, and updates. Each delegated task names its base commit, owned/forbidden paths, expected API, focused verification, and required evidence. The root implements only integration glue or small conflict fixes.

Consume completion events promptly and inspect results before follow-up. Reviewers are read-only. One integration owner exclusively controls `:webApp`, navigation, root/settings/build files, and shared `:core:ui-web` contracts.

Every checkpoint should contain only:

> **Observed:** verified fact. **Hypothesis:** current explanation. **Next falsifier:** cheapest decisive test. **Stop/escalate:** explicit condition.

## What went wrong in WEB-02

- It was a mega-ticket: design system, two feature UIs, native interop, auth, CRUD, persistence, accessibility, visuals, three browsers, Android/root gates, and live acceptance.
- Mocks created confidence before proving the exact live credential payload and subsequent persistence transaction.
- Architecture review and specialist escalation happened late, after expensive speculative cycles.
- Binaryen production distributions, full browser matrices, and large Android/root gates were repeated after small production or test-only changes.
- Fixed-coordinate clicks/readiness assumptions created flaky WebKit evidence instead of using semantic bounds or explicit native DOM contracts.
- Agent completion events were not always consumed promptly, causing idle time, duplicated investigation, and unnecessary reruns.
- The live runner/parser was hardened late (`:`/`=`, casing, aliases, duplicates/blanks, child environment, cleanup), causing avoidable approval/execution cycles.
- Compose/Wasm canvas text-field focus produced a zero-delay credential-entry race. The final solution uses one native HTML login form and synchronously commits both values before submit.
- AndroidX Preferences/DataStore on Wasm attempted a non-null cast when removing an absent key. WEB-02 fixed project-owned reachable removals by checking key presence first.

## Fast-feedback protocol

1. **Tier 0 — setup (≤5 minutes):** confirm base/status, reserve paths, define one acceptance journey, and identify the first provider/platform boundary.
2. **Tier 1 — iteration (≤15 minutes):** use development Wasm, focused unit/Compose tests, and one Chromium 1280×720 journey. After two failed hypotheses or 15 minutes, assign a specialist. No Binaryen, full matrix, or root `check`.
3. **Early live proof:** when the smallest auth/network/persistence vertical slice exists, run one redacted provider smoke before broad mocks; then return to mocks.
4. **Tier 2 — review:** freeze production code; perform read-only architecture, boundary, lifecycle/leak, accessibility, and security review before expensive gates.
5. **Tier 3 — candidate:** run one production/Binaryen distribution, one three-browser/two-viewport matrix, and one combined Android/root gate. Production changes create a new candidate and return to focused verification.
6. **Test-only correction:** rerun only the failed/affected cases. Report the original matrix result and focused rerun separately; never convert them into an unexecuted full-pass claim.
7. **Final live proof:** after every other gate, request/run one provider acceptance and cleanup.

Evidence states command, build kind, browser/viewport, counts, failures/skips, and focused versus complete. Follow `docs/kmp/web-testing.md`; avoid bare coordinates. Inspect screenshots—file presence is not visual approval.

## Rewrite the remaining tickets

### WEB-03 — browse milestone

- **WEB-03A — contracts/integration shell:** create module shells; freeze `StreamCoreWeb*`, route/focus, fixtures, history/deep-link shapes, top chrome, and explicit temporary Player route. Integration owner controls all shared files.
- **WEB-03B — Home:** only `:feature:home:ui-web` and local tests/showcases.
- **WEB-03C — Search:** only `:feature:search:ui-web`, native input, debounce/cancellation, and local tests/showcases.
- **WEB-03D — Library:** only `:feature:library:ui-web`, profile isolation/mutations, and local tests/showcases.
- **WEB-03E — Details:** only `:feature:details:ui-web`, ID loading, mutations, trailer, and local tests/showcases.
- **WEB-03F — integration gate:** merge B→C→D→E, wire routes, validate history/reload/logout/focus restoration, run single full candidate gates, then final live TMDB browse journey.

B–E branch from the immutable WEB-03A contract commit. With four total agent slots, keep the orchestrator active and run three feature owners concurrently; start the fourth when a slot completes.

### WEB-04 — playback/release

- **WEB-04A — contract/engine:** freeze provider-neutral APIs; own `:playback:web`, Shaka/HTML video interop, state/commands, cleanup, and fake-engine tests. Shared changes must remain Media3-implementable.
- **WEB-04B — player UI:** branch from the contract-freeze commit; own only `:feature:player:ui-web` and fake-session UI/focus tests.
- **WEB-04C — release/test:** from the same commit, own browser fixtures, release/CORS/CSP/cache docs, leak/error/autoplay scenarios, and artifact checks; no production feature/engine edits.
- **WEB-04D — final integration:** integration owner wires `:webApp`, merges A→B→C, replaces the placeholder, runs one production/full-browser/Android-root candidate gate, then coordinates the required manual Safari/macOS pass.

Parallelize reasoning, reviews, and disjoint edits; serialize shared changes and execution. Never run concurrent Gradle, webpack, Playwright-server, or Binaryen jobs against shared caches. Use a build queue. Merge only reviewed commits in declared waves; feature agents do not resolve cross-module conflicts.

## Credential workflow

Keep secrets in ignored local files and use `webApp/e2e/run-live-auth.ps1`; never read, print, copy, commit, screenshot, or prompt them. Iterate with mocks. Ask at most once, only when the final live command and non-live gates are ready. Mandatory action-time approval for external credential transmission cannot be bypassed; user-manual wrapper execution means Codex transmits nothing and reads only the redacted result. Consider TMDB redirect authorization in a future web ticket, never as silent WEB-03/04 scope.

## Copy-ready opening prompt

> Use GPT-5.6 Sol with Ultra reasoning. Read `AGENTS.md`, `docs/kmp/ORCHESTRATION-HANDOFF.md`, the KMP ticket README, WEB-02 evidence from `codex/web-02-login-profiles`, and WEB-03/04. Act strictly as orchestration/integration owner. First verify both worktree states, perform a read-only WEB-02 review, and update/commit the backlog protocol and split tickets described in the handoff. Preserve the pre-existing `.kotlin/` directory and backend-agnostic boundaries. Do not start WEB-03 code or run live credentials until those documentation changes and WEB-02 merge decision are complete. Delegate disjoint modules in maximum-safe parallel waves, serialize all Gradle/Binaryen execution, and report only evidence-backed results.
