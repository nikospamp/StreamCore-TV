# KMP-07 Android parity evidence

## Status and identity

- Branch: `codex/kmp-07-android-parity`.
- Prerequisite/base: `41a54ede41914886de2213fb3ef8948d44f55f4e` (integrated KMP-06).
- Verified production/test remediation commits:
  - `74d65cd` — common vector runtime compatibility and coordinated AndroidX Test alignment.
  - `a3eacb6` — provider Compose Resource packaging in TMDB and ClientB UI AARs/APKs.
  - `4c7cdaf` — deterministic tablet parity assertions matching the accepted app-shell/source-set mapping.
- Verification date: 2026-09-01 (Europe/Athens).
- Target architecture: backend-agnostic Android/KMP Phase 1. No Wasm/web target or WEB-01 implementation is present.
- Acceptance status: pending only the ticket-required controlled physical-device before/after benchmark campaign. Host/release, installed
  persistence, automated phone/tablet/TV gates, and authenticated TMDB/ClientB phone/tablet/TV journeys are green.

## Integration remediations

The first phone connected campaign exposed two KMP-06 integration regressions and one inherited test-graph mismatch that host compilation could not detect:

1. Eight common Compose vector files referenced `@android:color/white`. Compose Resources rejected the Android framework reference at runtime.
   Each fill is now the exact literal equivalent `#FFFFFFFF`; path geometry is unchanged, and all common Compose Resources scan clean for
   `@android:` references.
2. Modules without a direct Espresso dependency selected Compose UI Test's old transitive `espresso-core:3.5.0`, `androidx.test:runner:1.5.0`, and
   `androidx.test:core:1.5.0`. API 36.1 failed through the removed reflective `InputManager.getInstance()` path. All feature device-test modules now
   use the existing catalog `espresso-core:3.7.0`; dependency insight proves aligned runner/core `1.7.0`. No production dependency changed.
3. Provider UI Compose Resources were generated but absent from Android AAR/APK assets. Both provider UI Android-KMP targets now explicitly enable
   Android resource processing. Archive inspection and restored-session launches prove TMDB/ClientB provider assets are packaged.

Two connected assertions were made deterministic without changing production behavior: the mobile kids-chip assertion uses unmerged child
semantics and a visible one-profile fixture; tablet Search replaces the complete query instead of relying on cursor placement. The tablet Home
assertion now records the accepted KMP-00C mapping: top-level Search belongs to the app navigation rail, so `TabletHomeScreen` does not expose a
screen-owned Search action.

## Host and release gate

All artifact-producing commands used Android Studio JBR, the existing dependency cache, `--no-parallel`, and a command-line-only 4 GiB Gradle heap.
No repository memory setting changed.

| Command | Result |
|---|---|
| `:app:assembleTmdbDebug` | Pass |
| `:app:assembleClientBDebug` | Pass; fixed-tree rerun 742 actionable tasks |
| `:app:assembleTmdbReleaseR8` | Pass after all production fixes; 1,384 actionable tasks |
| `:app:assembleClientBReleaseR8` | Pass after all production fixes; 1,383 actionable tasks |
| `check -PverifyDesignTokensLogFiles=true --continue` | Pass after all fixes; 1,663 actionable tasks |
| `testAndroidHostTest` | Pass; 24 common-test modules map to 24 Android host-test targets |
| `lint` | Pass |
| both app flavor `*KoinGraphTest` suites | Pass; strict TMDB and ClientB graphs resolve without overrides/duplicates |
| required mobile/tablet/TV Android-test compilation tasks | Pass |
| `:benchmark:assemble` | Pass; benchmark and benchmarkR8 producer APKs built |
| `:baselineprofile:assemble` | Pass; profiles were not regenerated |

After the durable linked-worktree preflight change, the configured TMDB debug and releaseR8 builds also pass with
`-PrequireTmdbRuntimeConfig=true`; the releaseR8 rerun reported 1,385 actionable tasks. The final credential-free root check passed in 1m 15s with
1,663 actionable tasks. Combined final `:benchmark:assemble :baselineprofile:assemble` passed with 262 up-to-date tasks and no regeneration.

Root guards report:

- `verifyKmpConventionPlugins`: two convention-only fixtures.
- `verifyKmpDependencyCompatibility`: 14 locked common and Android coordinates (14/12 artifact files).
- `verifyKmpAndroidCompilerFlags`: 21 Compose-KMP Android compile tasks with `-Xlambdas=class`.
- `verifyKmpTestTargets`: 24 common-test modules and 24 host-test targets; `:core:domain` is the explicit compile-only exemption.
- `verifyDesignTokensLogFiles`: 303 production Kotlin files; zero violations.
- Hilt/Dagger scan: no `dagger.*`, Hilt, `javax.inject`, or Hilt dependency residual.
- Wasm scan: no `wasmJs` target.
- SDK declarations: compile SDK 37; app/benchmark/baseline-profile target SDK 36.

### Executed host tests

The clean root XML inventory is 64 suites and **265 tests**, with zero failures, errors, or skips. This exactly matches KMP-06 and is +51 versus the
corrected accepted KMP-00 total of 214. Migrated suites execute as `testAndroidHostTest`; app flavor tests and the Android-only Player platform UI
suites retain their Android unit-test task names. No expected task executes zero tests.

| Owning module | Current task | KMP-00 | KMP-07 | Delta |
|---|---|---:|---:|---:|
| `:app` ClientB | `testClientBDebugUnitTest` | 23 | 25 | +2 |
| `:app` TMDB | `testTmdbDebugUnitTest` | 23 | 25 | +2 |
| `:client:clientB:data` | `testAndroidHostTest` | 25 | 25 | 0 |
| `:client:clientB:player` | `testAndroidHostTest` | 0 | 1 | +1 |
| `:client:clientB:ui` | `testAndroidHostTest` | 3 | 3 | 0 |
| `:client:tmdb:data` | `testAndroidHostTest` | 38 | 46 | +8 |
| `:client:tmdb:player` | `testAndroidHostTest` | 0 | 1 | +1 |
| `:client:tmdb:ui` | `testAndroidHostTest` | 4 | 4 | 0 |
| `:core:data` | `testAndroidHostTest` | 0 | 11 | +11 |
| `:core:tracing-api` | `testAndroidHostTest` | 0 | 4 | +4 |
| `:feature:details:domain` | `testAndroidHostTest` | 2 | 3 | +1 |
| `:feature:details:ui-common` | `testAndroidHostTest` | 14 | 14 | 0 |
| `:feature:home:domain` | `testAndroidHostTest` | 1 | 3 | +2 |
| `:feature:home:ui-common` | `testAndroidHostTest` | 14 | 15 | +1 |
| `:feature:library:data` | `testAndroidHostTest` | 5 | 7 | +2 |
| `:feature:library:domain` | `testAndroidHostTest` | 4 | 5 | +1 |
| `:feature:library:ui-common` | `testAndroidHostTest` | 3 | 3 | 0 |
| `:feature:login:domain` | `testAndroidHostTest` | 3 | 5 | +2 |
| `:feature:login:ui-common` | `testAndroidHostTest` | 5 | 5 | 0 |
| `:feature:player:data` | `testAndroidHostTest` | 3 | 5 | +2 |
| `:feature:player:ui-common` | `testAndroidHostTest` | 11 | 11 | 0 |
| `:feature:player:ui-mobile` | `testDebugUnitTest` | 12 | 12 | 0 |
| `:feature:player:ui-tv` | `testDebugUnitTest` | 2 | 2 | 0 |
| `:feature:profiles:domain` | `testAndroidHostTest` | 0 | 9 | +9 |
| `:feature:profiles:ui-common` | `testAndroidHostTest` | 3 | 3 | 0 |
| `:feature:search:data` | `testAndroidHostTest` | 2 | 4 | +2 |
| `:feature:search:domain` | `testAndroidHostTest` | 2 | 2 | 0 |
| `:feature:search:ui-common` | `testAndroidHostTest` | 12 | 12 | 0 |
| **Total** |  | **214** | **265** | **+51** |

## Device matrix

Only one emulator ran at a time.

| AVD | Serial | API | Resolution | Fingerprint |
|---|---|---:|---:|---|
| `Medium_Phone_API_36.1` | `emulator-5554` | 36 | 1080x2400 | `google/sdk_gphone64_x86_64/emu64xa:16/BE4B.251210.005/14574095:user/release-keys` |
| `Medium_Tablet` | `emulator-5554` | 36 | 2560x1600 | `google/sdk_gphone64_x86_64/emu64xa:16/BE4B.251210.005/14574095:user/release-keys` |
| `Television_1080p` | `emulator-5554` | 31 | 1920x1080 | `google/sdk_google_atv_x86/generic_x86:12/STT9.221129.002/9351024:user/dev-keys` |

### Connected tests

| Device | Suites | Tests | Result |
|---|---|---:|---|
| Phone | Core UI plus Login, Profiles, Home, Search, Library, Details, Player | 54 | 54/54 pass |
| Phone | Focused Coil cold/warm disk-cache instrumentation | 1 | Pass: `NETWORK -> DISK`, one request (`coldMs=595`, `warmMs=21`; timings informational) |
| Tablet | Login, Profiles, Home, Search, Library, Details | 23 | 23/23 pass |
| TV | Login, Profiles, Home, Search, Library, Details, Player | 65 | 65/65 pass |
| TV | App navigation/logout/background/cache tests | 23 | 23/23 pass |

Total recorded connected coverage is **166 tests**, all passing with zero skips.

### Manual smoke completed

ClientB passed on phone, tablet, and TV:

- login and restored-session boot;
- provider profile artwork/resources, selection, Home, Details, Like/My List, Search, Library, and playback;
- phone player prepare/progress, pause/resume, seek, settings, exit, progress resume, and PiP request path;
- tablet restored pre-migration ClientB auth, adaptive navigation rail, landscape/portrait/landscape configuration round-trip, Details, and player;
- TV form/profile D-pad order, drawer Left/Right transfer, Search, two-stage Back handling, exact Home Details-focus restoration, Details/player focus graph,
  focused Back exit, and persisted Resume state.

Authenticated TMDB also passed on phone, tablet, and TV:

- cold login, restored-session relaunch, profile selection/focus, and logout cancel/Back/confirm;
- remote Home content/artwork, cold/warm phone artwork, Search, Library, Details, recommendations, Like/My List, and trailer handlers;
- phone player prepare/pause/seek/settings/exit/Resume and system-confirmed/visually captured PiP;
- tablet adaptive rail, portrait/landscape state round-trip, Details, and player;
- TV drawer Left/Right transfer, deterministic profile/content/player focus, exact Home Details return focus, trailer/YouTube return, seek/timeline,
  settings, focused Back exit, Resume, and restored-session/logout paths.

Ignored local screenshots are under `build/kmp-07-evidence/{phone,tablet,tv}` and contain no credentials. The deterministic Coil test proves the
Coil Ktor3 cold-network/warm-disk path. Authenticated phone screenshots prove remote cold/warm artwork and successful PiP without exposing local
configuration values.

### Baseline journey comparison

| Journey | KMP-00 | ClientB phone | ClientB tablet | ClientB TV | TMDB phone | TMDB tablet | TMDB TV |
|---|---|---|---|---|---|---|---|
| Login, logout, restored session | Pass | Pass | Pass | Pass | Pass | Pass (pre-migration restored auth) | Pass |
| Profile select/create/edit/delete | Pass | Pass/connected | Pass/connected | Pass/connected | Pass/connected | Pass/connected | Pass/connected |
| Home load/content/refresh/navigation | Pass | Pass | Pass | Pass | Pass | Pass | Pass |
| Search discovery/query/recents/result | Pass | Pass | Pass/connected | Pass | Pass | Pass | Pass |
| Library empty/content/mutation/isolation | Pass | Pass | Pass/connected | Pass/connected | Pass | Pass | Pass |
| Details/refresh/recommendations/mutations/trailer/back | Pass | Pass | Pass | Pass | Pass | Pass | Pass |
| Player prepare/play/pause/seek/settings/exit/resume/PiP | Pass | Pass except visible PiP confirmation | Pass | Pass | Pass including system-confirmed PiP | Pass | Pass (PiP N/A) |
| TV drawer/D-pad/focus/return restoration | Pass | N/A | N/A | Pass | N/A | N/A | Pass |
| Phone/tablet adaptive/orientation | Pass | Pass | Pass | N/A | Pass | Pass | N/A |
| Remote cold/warm artwork after Coil migration | N/A | Deterministic loopback cache pass | Provider-local artwork pass | Provider-local artwork pass | Pass | Pass | Pass |

## Persistence compatibility

Executable common tests seed and recreate the exact pre-migration stores/keys for:

- TMDB auth (`tmdb_auth.preferences_pb` and its four session/account keys);
- ClientB auth (`client_b_auth`, `is_logged_in`);
- Search (`search_history.preferences_pb`, `recent_searches_json`);
- Library (`library.preferences_pb`, `library_json`);
- Playback (`playback_progress.preferences_pb`, `entries_json`).

The installed compatibility campaign additionally rebuilt the exact accepted KMP-00 commit
`c2f90825bd336489f98361c8ba3a124800a51d04`, installed its ClientB debug APK on a disposable API-36 phone AVD, and populated all four persisted
surfaces through the legacy UI:

- logged-in ClientB auth;
- recent Search query `Archive`;
- Liked and My List membership for `The Last Archive`;
- playback progress observed at 0:55 before exit.

Before upgrade, the app sandbox contained the auth, Search, Library, and Playback DataStore files (20, 87, 555, and 807 bytes respectively). The
current KMP-07 ClientB APK was installed in place with `adb install -r`; first-install time remained unchanged and last-update time advanced. The
current app launched directly to profiles, showed `Archive` under Recent searches, and showed `The Last Archive` in Continue Watching, Liked, and
My List. All four files retained the same sizes after readback. The disposable AVD and detached KMP-00 worktree were removed after evidence capture.

Separately, the preserved tablet launched directly into ClientB profiles using pre-existing auth, then retained Home state across orientation and
exercised Details/player.

## APK and Baseline Profile evidence

Configured TMDB artifacts intentionally omit APK hashes because local runtime configuration is compiled into BuildConfig and security policy
forbids hashing credentials or account identifiers. The value-redacted linked-worktree preflight and configured TMDB debug assembly pass. ClientB
debug/releaseR8 and credential-free TMDB debug/releaseR8 assemblies passed earlier in the same gate.

Generated profiles are byte-for-byte unchanged from the accepted baseline:

| File | Size | Lines | SHA-256 |
|---|---:|---:|---|
| `baseline-prof.txt` | 4,512,603 | 39,774 | `E83C0A0E3019B551E8595D168A356FBFBA8F400DFC9F9E82AA07FFCDEB3648DE` |
| `startup-prof.txt` | 2,274,211 | 22,529 | `1ACE4DEAFDDAC2F2D9C5B9ED303209085DCD0D1C8271E21E02C8B1086A1DA372` |

The fixed TMDB releaseR8 APK contains `assets/dexopt/baseline.prof` and `baseline.profm`. Provider avatar Compose Resources are present in both
provider AARs and the corresponding flavor APKs. Profile generation was intentionally not rerun.

## Performance comparison

The ticket-required controlled before/after campaign remains blocking because no physical Android 14+ device is connected. Emulator timing is not
performance evidence and was not substituted. The accepted physical-device campaign remains `docs/performance/samsung-controlled-v2-final.md`
(320/320 journeys, 32/32 cells); it is the before reference but predates KMP-07. The interrupted `BaselineProfileMode.Require` diagnostic remains
non-reportable and was not reused.

Safest completion path: connect an Android 14+ physical device, authenticate the isolated benchmark app, pin one exact content tag after preflight,
and run new, non-overwriting identities through `tools/performance/run-navigation.ps1` and `run-campaign.ps1`. Record APK/driver hashes, device
fingerprint, 32/32 cells, 320/320 journeys, and the informational before/after comparison. Do not use an emulator or append to the accepted run.

## Remaining stop condition

The existing TMDB token was recovered locally and the account ID was retrieved through the explicitly authorized TMDB login/session flow. Both
temporary sessions were deleted. Only the token/account ID are present in ignored primary `local.properties`, with `sdk.dir` preserved; no values,
hashes, or paths are committed or recorded. `:app:verifyTmdbRuntimeConfig` passes when referencing that ignored file and fails value-safely when
configuration is absent. Authenticated phone and tablet journeys are green.

The TMDB manual matrix is complete. The controlled physical-device campaign above is now the sole remaining acceptance item and is explicitly
deferred, not waived. KMP-07 remains Pending; do not merge or begin WEB-01 until the campaign completes or the ticket is explicitly amended.
