# Samsung controlled mobile-navigation findings

## Result and validity

The controlled campaign is complete: **320/320 measured journeys, 32/32 cells**, with ten iterations per cell. Every selected representative trace
reproduces its native Macrobenchmark frame CPU and deadline-overrun samples. No debug results, interrupted batches, battery-rejected runs, old Samsung
pilots, or OnePlus diagnostics are pooled here.

Target architecture remains backend-agnostic. No production optimization was applied during this baseline.

- Device: Samsung Galaxy S23 Ultra (`SM-S918B`), Android 16 / API 36, approximately 120 Hz.
- Dataset: profile `Nikos`, `home:content:continue-watching:1516698`.
- Builds: release-derived non-debuggable unminified `benchmark` and same-source R8/resource-shrunk `benchmarkR8`.
- Compilation: `None` versus `Partial(BaselineProfileMode.Disable, warmupIterations = 3)`.
- Thermal status: required 0 before setup, at measurement start, and after the 500 ms capture tail.
- Unminified APK: `2EFB344BEC94A2EFA65F51C60DEB7C1267A0C4CD38E8F58623886484598C7461`.
- R8 APK: `DB6C77092091D1E36C1BA57DF61A5BE7926D39770BCC98F78EDEF0A9C40C8CCE`.
- Driver: `6A3B9FC39E7D07224A2FEA9148761C2CB0163C805BFD40620D493B85FDF0FF4C`.

Evidence:

- [Campaign journal](../../benchmark-results/navigation-20260829-samsung-controlled-v2/campaign.json)
- [Complete P50/P95/P99 distributions and trace links](../../benchmark-results/navigation-20260829-samsung-controlled-v2/summary/navigation.md)
- [32 native-verified representative trace summaries](../../benchmark-results/navigation-20260829-samsung-controlled-v2/trace-evidence)
- [Frozen source patch](../../benchmark-results/navigation-20260829-samsung-controlled-v2/source.patch)

## Frame CPU P95

Milliseconds; frames are pooled only inside the named ten-iteration cell.

| Journey          | Entry    | Unminified None | Unminified Partial | R8 None | R8 Partial |
|------------------|----------|----------------:|-------------------:|--------:|-----------:|
| Profiles → Home  | First    |           25.34 |              12.84 |   17.71 |      16.27 |
| Profiles → Home  | Repeated |           24.07 |              10.77 |   16.44 |      11.23 |
| Home → Details   | First    |           34.08 |              15.57 |   20.16 |      14.91 |
| Home → Details   | Repeated |           28.84 |              13.04 |   20.94 |      11.15 |
| Player → Details | First    |           23.14 |              15.16 |   16.60 |      13.27 |
| Player → Details | Repeated |           23.03 |              12.69 |   13.81 |      12.54 |
| Initial Search   | First    |           29.84 |              12.32 |   17.30 |      12.12 |
| Initial Search   | Repeated |           33.63 |              13.83 |   25.31 |      17.83 |

The automated report contains frame CPU P50/P99, deadline-overrun P50/P95/P99, readiness wall distributions, frame counts, and exact source/trace
paths.

## Conclusions

### 1. Compilation materially explains first-use and navigation stalls

Warmup-based Partial reduces unminified frame CPU P95 by **34–59%** across all eight journey/entry pairs. On R8 it reduces P95 by **8–47%**.
Deadline-overrun tails generally move in the same direction.

This meets the investigation criterion for an app Baseline Profile experiment. It does **not** claim that the three warmup-generated ART profiles are
the benefit of a shipped Baseline Profile; generation and a `BaselineProfileMode.Require` before/after measurement are still required.

### 2. R8 mainly improves uncompiled execution

Under `None`, R8 reduces frame CPU P95 by **25–42%** in every journey/entry pair. Under Partial, the difference is mixed (approximately 15% better to
29% worse), with several cells nearly equal. R8 is justified as a production release candidate for code size and uncompiled/JIT behavior, but the
perceived benchmark speedup cannot be assigned entirely to R8.

Build order alternated by journey but was not reversed within each journey. Small build differences remain order-sensitive; focused rollout
verification should reverse order when the effect is small.

### 3. Player exit has a separate synchronous tail

Compilation lowers Player P95, but frame CPU P99 remains **57–92 ms** across the eight Player cells. Native-verified controlled traces show:

- `SC.Player.orientation.restore`: **43–62 ms**;
- `SC.Player.release`: **35–63 ms**, separate from orientation;
- width range **823 → 384 dp**, with transient tablet and mobile Details composition;
- frame-cache counter at zero after eviction in every selected capture.

This confirms the [Player exit optimization design](player-exit-optimization-design.md): restore orientation idempotently before `popBackStack()`,
wait for configuration settlement with a bounded fallback, then navigate. Keep bars/flags disposal as fallback and leave Media3 release on its
application looper. Preserve PiP, progress persistence, adaptive sizing, and shared effects.

### 4. Home and Details need descendant layout attribution before source changes

Uncompiled selected frames retain broad measure/layout and recomposition intervals; Partial usually shrinks them substantially. Existing compiler
reports already mark `MobileHomeScreen` and `HomeHeroCard` restartable/skippable, and current lazy keys/content types are adequate. The evidence does
not support blanket stability annotations, collection replacement, removing shared bounds, or replacing lazy containers.

Follow [Home optimization design](home-optimization-design.md): add benchmark-only descendant measure/place and publication-reason markers, then test
one conditional change at a time. Leading candidates remain coalescing a progress-only publication before initial backend rows, or replacing hero
`BoxWithConstraints` only if it owns the slow measure slice.

### 5. Search readiness and IME work are different measurements

First-entry selected traces contain `SC.Search.IME.request` of **9.7–13.1 ms** plus asynchronous discovery of **54–182 ms**. Repeated-entry traces
contain neither. First readiness medians are approximately **1.5–1.9 s**, versus **0.28–0.42 s** repeated, but these paths intentionally differ in
autofocus, discovery, caches, and UIAutomator waits.

Partial lowers Search frame CPU P95 substantially, including repeated entry where no IME request occurs. Do not treat delaying the keyboard as the
main performance fix. If Search is optimized later, measure focus request → IME-visible separately while preserving first focus and Back-to-dismiss
behavior.

## Ranked implementation plan

| Rank | Change                                                                                       | Expected value                                                                                          | Risks / required verification                                                                                                          |
|------|----------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------|
| 1    | Add Baseline Profile generator and ship profiles for startup plus the four measured journeys | Broad reduction in first-use/JIT frame tails                                                            | Measure `Require` versus `Disable`; verify profile packaged, profile isolation, Search focus, Player/PiP, and no startup regression    |
| 2    | Implement Player orientation-before-pop sequencing                                           | Remove 43–62 ms orientation Binder work from destination composition and avoid transient tablet Details | Rotation lock/sensor, tablet/multi-window, PiP, bars, progress, release-once, shared transition, focused before/after Player benchmark |
| 3    | Enable R8/resource shrinking for a production release candidate                              | 25–42% P95 improvement in uncompiled cells plus APK/code-size benefit                                   | Keep separate rollout; validate serialization/navigation/DI/proguard rules, mapping upload, all journeys and release smoke tests       |
| 4    | Add Home descendant layout/publication tracing, then apply one proven candidate              | Locate and reduce residual layout waves without speculative Compose changes                             | Instrumentation perturbation, profile isolation, carousel timing/indicator, rows/progress, shared-bound screenshots                    |
| 5    | Add Search IME-visible timing before any focus scheduling change                             | Separate keyboard latency from discovery/navigation                                                     | Preserve autofocus and Back semantics; do not infer benefit from readiness alone                                                       |

## Regression gate for production changes

Use new artifact hashes and a new treatment campaign. Keep the same device, dataset, entry semantics, compilation mode, thermal boundaries,
animations, and cache policy. For the targeted journey require a lower frame CPU/deadline-overrun P95, no meaningful P50/readiness regression,
structural removal of the attributed work, and all functional/PiP/profile/shared-transition checks. Do not pool treatment results with this baseline.
