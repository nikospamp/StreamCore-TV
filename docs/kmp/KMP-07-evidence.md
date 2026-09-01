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
- Acceptance status: pending completion of authenticated TMDB manual device journeys. Host/release and automated phone/tablet/TV gates are green.

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

Ignored local screenshots are under `build/kmp-07-evidence/{phone,tablet,tv}` and contain no credentials. The deterministic Coil test proves the
Coil Ktor3 cold-network/warm-disk path. Remote authenticated TMDB cold/warm artwork remains part of the pending TMDB manual journeys.

## Persistence compatibility

Executable common tests seed and recreate the exact pre-migration stores/keys for:

- TMDB auth (`tmdb_auth.preferences_pb` and its four session/account keys);
- ClientB auth (`client_b_auth`, `is_logged_in`);
- Search (`search_history.preferences_pb`, `recent_searches_json`);
- Library (`library.preferences_pb`, `library_json`);
- Playback (`playback_progress.preferences_pb`, `entries_json`).

The tablet manual run additionally launched directly into ClientB profiles using auth persisted by the pre-KMP installation, then preserved active
Home state across orientation and exercised Details/player. Phone Library showed persisted playback, Liked, and My List entries after player exit.

## APK and Baseline Profile evidence

| Artifact | Size | SHA-256 |
|---|---:|---|
| TMDB debug APK | 23,161,867 | `EBB90E0A704C853D80E4588A67E2BBFE02144066B95A0491342B58538CE5F156` |
| ClientB debug APK | 23,071,962 | `FE736B1E368A62AEAE29DD6C17948BF3C1C89842DBF8CE2893E67FF54471CF86` |
| TMDB releaseR8 unsigned APK | 4,010,309 | `C743FD20FDA3314C0199CFD61BDE971247A287E639AC9EA52305582B95095C0D` |
| ClientB releaseR8 unsigned APK | 3,946,610 | `162ED5AF9AA0A9DF3CC625982654D2FFFA18BFF62A8C93ECF3B486F085A19B82` |

Generated profiles are byte-for-byte unchanged from the accepted baseline:

| File | Size | Lines | SHA-256 |
|---|---:|---:|---|
| `baseline-prof.txt` | 4,512,603 | 39,774 | `E83C0A0E3019B551E8595D168A356FBFBA8F400DFC9F9E82AA07FFCDEB3648DE` |
| `startup-prof.txt` | 2,274,211 | 22,529 | `1ACE4DEAFDDAC2F2D9C5B9ED303209085DCD0D1C8271E21E02C8B1086A1DA372` |

The fixed TMDB releaseR8 APK contains `assets/dexopt/baseline.prof` and `baseline.profm`. Provider avatar Compose Resources are present in both
provider AARs and the corresponding flavor APKs. Profile generation was intentionally not rerun.

## Performance comparison

No physical Android 14+ device was connected, so no new controlled campaign was run. Emulator timing is not performance evidence. The accepted
physical-device campaign remains `docs/performance/samsung-controlled-v2-final.md` (320/320 journeys, 32/32 cells); it is informational and
predates KMP-07. The interrupted `BaselineProfileMode.Require` diagnostic remains non-reportable and was not reused.

## Remaining stop condition

Authenticated TMDB phone/tablet/TV manual journeys cannot run from an empty runtime API configuration. The authorized ignored
`docs/credentials/tmdb.txt` contains interactive credentials only; `tmdbReadAccessToken` and `tmdbAccountId` were absent from the worktree,
primary `local.properties`, selected Gradle user home, environment, and emulator DataStore. No secret was printed, logged, copied, staged, or
committed. KMP-07 must not be marked accepted and WEB-01 must not start until an approved local source supplies those two Gradle properties and the
TMDB manual matrix passes.
