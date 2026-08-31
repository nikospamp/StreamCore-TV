# Home layout optimization design

## Recommendation

Do not change Home production behavior from the current owner-level traces. First add benchmark-only measure/place attribution and state-publication
markers, then run one candidate at a time against the fresh controlled Samsung baseline. The leading conditional candidate is to avoid publishing a
progress-only Home tree before backend rows arrive; the leading UI candidate is to replace the hero's `BoxWithConstraints` subcomposition only if the
hero owns the long measure slice.

Keep the target architecture backend-agnostic. Preserve the 5-second carousel interval, one stationary standalone indicator, shared bounds, row
order/content, reactive playback progress, and profile isolation.

## Evidence boundary

- The controlled Samsung traces retain broad `AndroidOwner:measureAndLayout` intervals in slow Profiles → Home frames, while warm-up compilation
  materially reduces their tails. This identifies layout traversal as a remaining owner category, not a specific composable.
- Recorded backend row mapping and progress decoding were below `1.2 ms` in the diagnostic evidence. Those operations do not explain the Main-thread
  layout slices and their probes have been removed.
- Compose source slices show composition work, not measure ownership. The full benchmark compiler report at
  `feature/home/ui-mobile/build/reports/compose/compileBenchmarkKotlin/` marks both `MobileHomeScreen` and `HomeHeroCard` restartable/skippable. This
  rules out unstable parameters as a reason to apply blanket stability changes, but it does not explain initial insertion or layout-phase
  invalidation.

Sources: [controlled Samsung final report](samsung-controlled-v2-final.md), [
`MobileHomeScreen`](../../feature/home/ui-mobile/src/main/kotlin/com/pampoukidis/streamcoretv/feature/home/mobile/home/MobileHomeScreen.kt), [
`HomeViewModel`](../../feature/home/ui-common/src/main/kotlin/com/pampoukidis/streamcoretv/feature/home/common/home/HomeViewModel.kt).

## Source assessment

| Area           | What the source establishes                                                                                                                                                                                                                                                                                                                        | Decision                                                                                                                            |
|----------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------|
| Lazy hierarchy | The outer `LazyColumn` has a hero key/type, row-id keys, and row-type content types. Nested `LazyRow`s use content-id keys and type-specific content types. These help reuse after insertion; they cannot skip initial composition/measurement.                                                                                                    | Do not change keys, content types, or replace lazy rows without descendant timing evidence.                                         |
| Hero           | `BoxWithConstraints` subcomposes a `HorizontalPager` during measure to derive `maxWidth`; the hero then has an explicit height.                                                                                                                                                                                                                    | Plausible measure owner, not yet demonstrated.                                                                                      |
| Carousel       | `StreamCorePagerCarousel` starts a fresh 5,000 ms interval and owns the single overlay indicator.                                                                                                                                                                                                                                                  | The timing/gesture/lifecycle contract is out of scope for optimization.                                                             |
| Indicator      | `animateDpAsState` is read into each dot's `Modifier.width`; page changes therefore recompose and remeasure the indicator for 220 ms. Its total bounds are currently stationary because one dot expands while another contracts.                                                                                                                   | Relevant only if a slow owner slice coincides with swipe/auto-advance. It does not explain initial page 0 without that correlation. |
| Shared bounds  | Profiles → Home resets `selectedContentKey` to null. Home content cards therefore take the clip-only branch; the active profile avatar still participates in shared bounds.                                                                                                                                                                        | Do not blanket-disable card or profile transitions. Attribute the top bar/avatar separately and preserve continuity.                |
| Images         | Artwork containers have explicit width/aspect ratio or fill an explicitly sized hero. `AsyncImage` fills those containers; image completion can invalidate composition/draw but should not establish a new parent size.                                                                                                                            | Keep image/data timing separate from layout ownership; no cache, crossfade, or request-policy candidate is supported.               |
| Progress/state | `observeProgress` and backend load independently call `mergedRows()`. If progress emits first, a progress-only row can be published and used as the featured fallback before backend rows cause a second content insertion. Later progress emissions rebuild the continue-watching row and the outer rows list while reusing provider row objects. | Trace publication order. Coalesce only if two content-bearing publishes lead to two expensive layout waves.                         |
| Profile switch | `activeProfileId` changes before old `backendRows`/`progressEntries` are cleared; a new progress emission can temporarily merge with the old profile's backend rows.                                                                                                                                                                               | Any state optimization must be generation/profile scoped. Add isolation coverage; do not introduce cross-profile caches.            |

The reusable `StreamCoreMobileContinueWatchingCard` and Home's private continue-watching card have similar fixed geometry. Consolidating them is a
consistency refactor, not a measured performance change.

## Required trace attribution

Create a separate diagnostic artifact; do not modify or reuse a frozen campaign identity.

1. Add a tracing-only `Modifier` that wraps `measurable.measure(constraints)` and placement with fixed, low-cardinality `SC.Home.measure.*` /
   `SC.Home.place.*` sections. It must inline to the original modifier when benchmark tracing is disabled. Do not use `onGloballyPositioned` as a
   duration proxy.
2. Trace these boundaries: top bar/profile avatar, pull-to-refresh content, outer lazy list, hero item, pager, indicator, continue-watching row, and
   shelf row by `RowType`. Start at row boundaries; per-card tracing is a second pass only for the owning row.
3. Emit fixed reason markers for `SC.Home.publish.loading`, `.backend`, and `.progress`, plus counters for publication generation, row count, visible
   content count, continue-watching count, window width, pager page, and whether profile/content shared bounds are active. Never put profile, row, or
   content IDs in labels.
4. Within each `AndroidOwner:measureAndLayout` slice, report which descendant measure/place sections occurred and their inclusive nesting. This
   distinguishes a full insertion pass from a targeted indicator/progress remeasure. Do not sum nested inclusive durations.
5. Correlate image success, state publication, recomposition/apply-changes, layout, and frame boundaries by timestamp. An async image span overlapping
   a frame is not Main ownership.
6. Only if the owning frame also shows redundant recomposition, correlate runtime change reasons with the existing full benchmark/release compiler
   reports for `:feature:home:ui-mobile` and `:core:ui`. Do not infer a fix from `List` types or annotate models speculatively; the screen and hero
   are already reported skippable.

## Ranked experiments

| Rank | Entry condition from the attributed trace                                                                                   | Single production experiment                                                                                                                                                                                 | Tradeoff / required checks                                                                                                                                                                                                                    |
|------|-----------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 1    | A progress publication creates content before backend publication, and both are followed by expensive outer-list/row layout | While the active profile's initial backend load is pending, store progress but do not publish a progress-only Home tree; publish the latest merged rows when backend succeeds. Keep later progress reactive. | May change when Continue Watching first appears. Test progress-before-backend, backend-before-progress, refresh with existing rows, failure, cancellation, and A→B profile switching. Do not wait indefinitely for a first progress emission. |
| 2    | `SC.Home.measure.hero`/pager owns most of the slow owner slice; row sections do not                                         | Replace `BoxWithConstraints` with a one-measure hero layout that derives the same `max(HeroMinHeight, width / LandscapeAspectRatio)` pixel height from incoming constraints, then measures the pager once.   | Custom measure code must handle bounded/unbounded constraints, density, font scale, split screen, and tablet-width mobile windows. Screenshot/bounds equality is required.                                                                    |
| 3    | Slow slices occur at page changes and `SC.Home.measure.indicator` repeats throughout the 220 ms transition                  | Keep the indicator's current total bounds and semantics, but animate dot width/color in draw rather than reading animated width in composition/layout.                                                       | A fixed-slot implementation would change bounds and is rejected. Preserve swipe, wrap, pause/resume, one indicator, 5-second fresh interval, and exact pre/post bounds.                                                                       |
| 4    | A later `.progress` publication causes broad shelf/hero layout rather than only the continue-watching card/progress bar     | Narrow the UI projection so unchanged provider rows and hero remain the same keyed items while only Continue Watching changes.                                                                               | Do not suppress meaningful progress, replace all collections, or add blanket memoization/`@Immutable`. Verify removal, reorder, duration changes, and shared source-row keys.                                                                 |

If the top bar/profile-avatar shared-bound section owns the slice, stop and design a transition-preserving experiment from that evidence. Removing
shared bounds is not an acceptable candidate. If nested lazy row measurement is the owner, retain lazy virtualization and inspect only the visible
owning row/card before changing the hierarchy.

## Focused regression coverage

- Extend `HomeViewModelTest` with controlled delayed backend/progress flows and state-history assertions: no progress-only featured fallback when
  coalescing, all backend rows/order retained, reactive progress add/update/remove retained, refresh does not blank content, cancelled profile A
  emissions never appear in profile B, and no A rows remain visible while B loads.
- Keep mapper coverage for featured fallback, shelf order, and Continue Watching filtering. Add no collection/stability test unless the production
  state contract changes.
- Keep `StreamCoreCarouselTest` assertions for 5 seconds, wrap, gesture reset, held-touch pause, lifecycle restart, shrinking counts, exactly one
  indicator, and stationary bounds. Add intermediate animation-bound checks if the indicator changes.
- Keep `MobileHomeScreenTest` coverage for growing/shrinking hero data, long text, pull-to-refresh, rows, Continue Watching, click routing, and
  standalone indicator. Add width/height equality across representative phone, narrow multi-window, and large-font constraints for a hero-layout
  change.
- Capture shared-bound screenshots for Profiles → Home and Home → Details; verify profile avatar, artwork/title continuity, row/source identity, and
  no flash from staged content.

## Exact before/after acceptance

Use the controlled Samsung baseline and a treatment built from the same source except for one experiment. Match device/OS/display mode, app/driver
protocol, fixture/profile, entry mode, compilation mode, thermal gate, animation scales, cache policy, and ten iterations per cell. Record new
artifact hashes and never pool the controlled baseline with partial or diagnostic runs.

Structural acceptance comes first:

- the baseline slow owner slices contain the predicted fixed descendant label;
- the treatment removes the duplicate publication or reduces that same owning descendant, rather than merely moving time to an unlabelled owner slice;
- unchanged branches no longer measure during the targeted pass when isolation is the claimed mechanism;
- row/progress counters, shared-bound flags, image-source markers, and carousel page/timer behavior remain valid.

For both first and repeated entry in the compilation cell under test, accept a production candidate only when all of the following hold within the
measured action window:

- `AndroidOwner:measureAndLayout` P95 improves by at least **25%**;
- the count of owner layout slices longer than the recorded display interval is reduced by at least **50%**;
- the attributed descendant's P95 improves by at least **30%** or the duplicate pass is absent by construction;
- frame-duration CPU P95 or deadline-overrun P95 improves by at least **15%**, and the other does not regress by more than **5%**;
- frame-duration CPU P50, deadline-overrun P50, and readiness P50 do not regress by more than **5%**;
- no correctness, profile-isolation, shared-transition, carousel, row, or progress regression occurs.

Treat P99 and ten-sample readiness tails as directional diagnostics, not population claims. A compiler-report delta, successful build, lower
recomposition count, or shorter async load alone is not acceptance.
