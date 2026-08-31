# Mobile navigation investigation

## Scope and invariants

Target: the affected physical phone, TMDB, mobile navigation. The target architecture remains backend-agnostic. This pass instruments and measures; it
does not change navigation, shared bounds, input timing, repositories, dispatcher placement, player behavior, profile isolation, or production release
minification.

The normal app is `com.pampoukidis.streamcoretv`. Both investigation variants use **`com.pampoukidis.streamcoretv.benchmark`**, signed with the local
debug key but **not debuggable**, and are shell-profileable. Installing either investigation variant updates the same separate package, preserving its
authentication and cache state. Never uninstall or clear app data between comparisons.

| Target variant    | R8                            | Resource shrinking | Runtime composition tracing |
|-------------------|-------------------------------|--------------------|-----------------------------|
| `tmdbBenchmark`   | Off (current release setting) | Off                | Benchmark only              |
| `tmdbBenchmarkR8` | On                            | On                 | Benchmark only              |

The companion `:benchmark` test APK is debuggable and self-instrumenting; it drives the separate target with UIAutomator. It is not the app being
timed. The frozen controlled campaign predates the app's generated Baseline Profile: its `None` and warm-up `Partial` cells do not measure the custom
profile now stored under `app/src/main/generated/baselineProfiles/`.

## Build

```powershell
.\gradlew.bat :app:assembleTmdbBenchmark :app:assembleTmdbBenchmarkR8 :benchmark:assembleTmdbBenchmark -PcomposeCompilerReports=true
```

Use the same JDK/Gradle home as Android Studio. The opt-in property writes compiler reports only for release/benchmark Kotlin tasks, under each
module's `build/reports/compose/<compileTask>/`. It does not disable or alter debug compilation. App and all affected Android UI libraries have
matching benchmark variants. Keep `app/build/outputs/mapping/tmdbBenchmarkR8/` with the R8 results for retracing.

Report mode deliberately uses full, uncached Kotlin compilation: incremental reports otherwise describe only changed files. The driver defaults to
`arm64-v8a` for the affected phone (`-PbenchmarkAbi` can change that for driver validation). Its Perfetto Java and native dependencies are pinned
together at 1.0.1, matching the target's tracing runtime.

`runtime-tracing` is confined to benchmark/profile-generation configurations. ProfileInstaller is a production dependency because it installs the
shipped Baseline Profile for local and non-Play distribution paths. Perfetto's native binary is in the driver, never production. The `:core:tracing`
facade uses compile-time-disabled inline helpers in normal variants. Its trace names contain operation names, not profile IDs, content titles,
credentials, or JSON payloads.

## Preflight / authentication

```powershell
.\tools\performance\run-navigation.ps1 -Serial <device> -Phase Preflight -RunId preflight
```

If preflight reports unauthenticated, open **StreamCore Benchmark**, sign in normally, and leave it at the profile picker. Do not copy a session from
the production app or use a debug-only authentication shortcut. Supply `-ProfileName` if the profile is not `Nikos`.

Read `benchmark-results/preflight/benchmark/macrobenchmark/navigation/preflight.json`. Pick a visible `home:content:<row>:<id>` from `contentTags` and
use that exact tag in **every** cell. Prefer a shelf card present before Continue Watching is populated. The driver fails if the pinned card
disappears rather than substituting another movie. Keep the catalogue, network, device settings, and phone orientation consistent. Do not interact
with the phone during measurement.

Preflight records model, Android/build fingerprint, display modes/refresh-rate state, battery, thermal service state, and animation scale. The run
wrapper records these again before/after each invocation plus APK SHA-256 and Git revision. Check that the phone is unlocked, not in battery saver,
not charging/heating, and has sufficient battery. No animation-scale or refresh-rate setting is changed by this workflow. Moderate-or-higher thermal
status fails the affected test; cool the phone and repeat it.

The current driver stops the target and waits up to three minutes for thermal status **0** before each sample, in 10-second increments outside
capture. It records cooldown duration, wakes the screen, then prepares the source. It never changes thermal policy or bypasses the lock screen. If the
phone cannot cool, the batch fails rather than publishing throttled measurements.

## Matrix

### Device compatibility

The controlled `None`/`Partial` campaign requires Android 14+ here. In Macrobenchmark 1.4.1, a non-rooted Android <14 device resets compilation by
uninstalling/reinstalling the target, deleting authentication and disk caches. Both the driver and host scripts reject that path before measurement.
Do not suppress it or copy production credentials.

For older phones, explicit `-Compilation Ignore` enables **diagnostics only** and leaves installed compilation unchanged. It does not satisfy the
controlled matrix or isolate an R8 benefit. Use a distinct run identity for each phone. Android <12 provides frame CPU duration but no
deadline-overrun metric; summaries select the representative trace using iteration CPU P95. For standalone trace extraction use
`analyze_trace.py --api-level 30 ...` on Android 11.

Validate an optimized-only diagnostic set using
`analyze_navigation.py <completed batch directories> --output <report directory> --diagnostic-variant benchmarkR8 --require-complete`. This requires
eight Ignore cells with ten iterations each; it never counts them toward the controlled None/Partial matrix.

The current driver requires thermal status 0 before setup, immediately before the measured action, and after the 500 ms capture tail. A nonzero
boundary fails the batch instead of publishing it as valid. The accepted controlled result is documented
in [the final Samsung report](samsung-controlled-v2-final.md). Older pilots and diagnostics remain excluded.

### Controlled campaign

Prefer the checkpointed campaign runner:

```powershell
.\tools\performance\run-campaign.ps1 -Serial <device> -ContentTag '<pinned tag>' -RunPrefix <unique-campaign>
```

It runs 40-sample journey/build batches, alternates build order by journey, and reuses the identical driver APK for both targets. Before every batch
it primes one progress/library fixture and the Search path via the real UI; the separate three-cycle cleanup screening need not be repeated for every
batch. Re-running the same command resumes only incomplete batches, requires unchanged APK hashes/dataset, and uses new attempt directories. Existing
evidence is never overwritten. A driver change requires a new campaign. Keep the phone unused and unlocked.

The lower-level commands remain useful for diagnosis:

```powershell
# Validate the driver before collecting the full comparison (not reportable as the final matrix).
.\tools\performance\run-navigation.ps1 -Serial <device> -ContentTag '<pinned tag>' -RunId smoke -Journey profilesToHome -Entry first -Compilation None -Iterations 1

# Ten first-entry and ten repeated-entry samples per compilation mode and journey.
.\tools\performance\run-navigation.ps1 -Serial <device> -ContentTag '<pinned tag>' -RunId matrix -Variant benchmark
.\tools\performance\run-navigation.ps1 -Serial <device> -ContentTag '<pinned tag>' -RunId matrix -Variant benchmarkR8
```

Each build runs four journeys × two entry modes × two compilation modes × ten measured iterations. `None()` removes compilation;
`Partial(BaselineProfileMode.Disable, warmupIterations = 3)` uses warmup-based ART compilation. Warmups are excluded from the exported readiness
samples. This is **320 measured journeys**, not 10 aggregate app tours. Run the opposite build order in a second campaign if a small difference could
be cache/thermal/order noise.

### Entry definitions

- **First:** restart the process, authenticate from the already persisted session, prepare only the source screen, and execute the journey once. This
  is a first journey in a fresh process, not a clean installation or a cold disk cache.
- **Repeated:** restart the process, execute the same journey once outside measurement, return through the normal UI, and measure its second
  execution. This controls process lifetime and provides one deliberate memory-cache/JIT warmup for the destination.
- **Player → Details:** “first” means the first *return from Player*. Details necessarily existed during setup. It is not a first-ever Details
  composition.
- **Search:** the first visit verifies focus/IME request. The primed visit dismisses IME with Back and restores the existing top-level state normally;
  repeated Search is allowed to retain its normal keyboard behavior. Do not force focus to make the two cases artificially identical.

### Journeys and readiness

| Journey          | Setup, outside capture                               | Measured navigation                                       |
|------------------|------------------------------------------------------|-----------------------------------------------------------|
| Profiles → Home  | Persisted authentication; same profile picker        | Choose profile → Home catalogue ready                     |
| Home → Details   | Home catalogue ready; locate pinned shelf card       | Select card → Details data ready                          |
| Player → Details | Same asset, Media3 ready, seek using the real slider | Back → mobile Details ready after orientation restoration |
| Initial Search   | Home catalogue ready                                 | Search tab → discovery ready, initial focus requested     |

After readiness, capture continues for 500 ms so cached destinations still include the 240 ms navigation transition and 80/150 ms delayed content.
This fixed tail is **excluded** from `readinessWallMs`. UIAutomator polling contributes to readiness wall time; it must never be described as CPU or
frame-stall time. Frame tracks and the small set of active, decision-specific trace markers separate asynchronous readiness from actual missed frames.

The current TMDB player resolves every asset to the repository's **Sintel DASH sample**, not full TMDB movie playback. Setup seeks to 10% through the
actual timeline to exercise resumable progress consistently, rather than accumulating forward seeks until the stream ends. Record that limitation when
discussing decoder/network behavior.

Disk caches and app storage are retained. Each process starts with cold memory caches; repeated entry primes its path once. Do not label a first visit
“cold image cache” merely because the process restarted.

## Evidence and analysis

```powershell
python -m unittest discover -s tools/performance -p 'test_*.py'
python tools/performance/analyze_navigation.py benchmark-results/matrix --output benchmark-results/matrix/summary --require-complete
python tools/performance/extract_campaign_evidence.py --help
python tools/performance/.tools/trace_processor -q tools/performance/attribution.sql <trace.perfetto-trace>
```

The analyzer emits per-journey/build/entry/compilation P50/P95/P99 for frame CPU duration, deadline overrun (when available), and readiness wall time.
Negative overrun means a frame met its deadline. It checks 32 matrix cells and their measured iteration counts; missing, duplicate, or failed cells
are not replaced with zeros. Ten wall samples do not establish a reliable population P99—treat tail estimates as exploratory and confirm suspicious
traces.

The campaign invokes the analyzer with only completed batch directories. The analyzer also rejects mixed target hashes within a build, mixed driver
hashes, or mixed content selection. `frameDurationCpuMs` is Macrobenchmark's UI-frame-start to RenderThread-end span, not a sum of scheduled CPU
execution. `analyze_trace.py` reconstructs the SDK's frame matching (including synthetic-frame-ID handling) to attach timestamps to the native
metrics. It was checked against the SDK's raw samples; inclusive slice durations must not be added together.

Use the slow-iteration indices and trace paths in `navigation.md` to inspect:

- initial composition versus later recomposition, `doFrame`, measure/layout/lookahead, RenderThread and GPU/deadline tracks;
- JIT/class loading/verification, GC and allocation pressure, distinguishing elapsed async spans from CPU work;
- `SC.TMDB.map.rows` / `SC.TMDB.map.details` relative to data readiness;
- `SC.Image.request` and `SC.Image.source.*`, plus image/decode thread activity;
- `SC.Player.persist`, `SC.Player.release`, `SC.Player.frameCacheBytesAfterRelease`, platform width changes, orientation restore, and subsequent
  Details composition;
- `SC.Search.IME.request` versus system IME/window/insets events.

Inclusive nested slice durations are not additive. A long asynchronous load span is not evidence that Main was blocked. R8 can change names,
initialization and allocation patterns; retain mapping and compiler reports for both builds. Composition tracing has overhead and changes APK size;
both sides use the same instrumentation. Use a follow-up tracing-disabled comparison if a measured difference is small.

## Fix-report rules

For each **confirmed** issue, provide the build/mode/journey, trace file and timestamp, relevant slice/thread/frame deadline evidence, affected
source, a narrow proposed change, tradeoffs, and regression coverage. Keep source hypotheses separate. Do not apply blanket stability annotations or
change collections/dispatchers based only on compiler classification. Kotlin strong skipping already applies.

Check counters and release slices before claiming retained subscriptions or bitmap/player leaks. `dumpsys meminfo` snapshots are screening evidence,
not proof of an object leak. Media3 calls must stay on their required application thread. If first-entry costs materially disappear with warmup
compilation while persistent layout/render work remains low, Baseline Profile generation is the next investigation step—not an unmeasured optimization
in this pass.

## Functional regression checks

Run existing Home, Details, Search, Profiles and Player tests with the tracing-disabled debug variants; verify the normal release block is unchanged.
On the physical benchmark installation, check profile isolation, back-stack restoration, shared-element continuity, Search keyboard/Back, persisted
playback position, player release, and PiP. A successful build or a driver preflight is not a performance conclusion.
