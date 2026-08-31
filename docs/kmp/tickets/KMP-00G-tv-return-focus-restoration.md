# KMP-00G — Fix TV Details Return-Focus Restoration

## Goal

Restore focus to the exact originating TV content item after returning from Details, while keeping drawer Back/close restoration deterministic.

## Context

KMP-00 verified visible focus, drawer order, drawer navigation, and drawer-close content restoration. It found one remaining failure: selecting the first
Search result, opening Details, and pressing Back returns with the drawer expanded/focused on Search; closing the drawer focuses the query field rather
than the originating result. `selectedContentKey` and item `FocusRequester`s already exist, so this ticket fixes ownership/timing rather than adding a
new navigation model.

## Dependencies and Parallelization

- **Depends on:** KMP-00C, KMP-00D, KMP-00E, and KMP-00F integrated and green.
- **Blocks:** KMP-00H and final KMP-00 acceptance.
- **Parallelization:** No. Run after all app navigation/player changes to avoid invalidating focus evidence.

## In Scope

- Search-result -> Details -> Back exact-item focus restoration.
- Equivalent Home/Library/Recommendation return-focus regression coverage.
- Drawer close/Back behavior and focus-request timing.
- TV focus instrumentation and evidence.

## Non-Goals

- No search data/query behavior change, drawer redesign, new navigation library, or global focus rewrite.
- No touch-platform changes except shared selected-content bookkeeping required for correctness.

## Implementation Tasks

1. Trace `selectedContentKey`, `selectedContent`, `TvNavigationDrawer`, `TvSearchRoute`, and result item `FocusRequester` lifetimes across typed navigation.
2. Keep the originating content key until the owning top-level surface successfully consumes it; do not clear it during Details/Player transitions.
3. On Details Back, return with the drawer closed unless it was explicitly open before navigation.
4. After Search content is present and the target key resolves, request focus exactly once on the matching item; avoid unconditional field-focus races.
5. Preserve selected query, result list position, recent-search state, and lazy-grid scroll position.
6. Keep drawer behavior green: Left opens with current destination focused; Back closes and restores prior content focus; Right closes and restores prior
   content focus; destination switching focuses the new surface's initial target.
7. Add focused tests for Search result return, Home card return, Library item return, recommendation/back chains, missing-key fallback, drawer Back, and
   destination switch.
8. Run the tests against TV API 31 and capture layout/focus state before navigation, in Details, immediately after Back, and after drawer interactions.

## Public API or Type Changes

- Prefer no public API change. If needed, add narrowly scoped consumed-focus callbacks/keys to TV route APIs; do not expose `FocusRequester` outside UI.

## Verification Commands

```powershell
.\gradlew.bat :feature:search:ui-tv:compileDebugKotlin
.\gradlew.bat :feature:search:ui-tv:compileDebugAndroidTestKotlin
.\gradlew.bat :feature:home:ui-tv:compileDebugAndroidTestKotlin
.\gradlew.bat :feature:library:ui-tv:compileDebugAndroidTestKotlin
.\gradlew.bat :feature:details:ui-tv:compileDebugAndroidTestKotlin
.\gradlew.bat :app:testTmdbDebugUnitTest --tests "*StreamCoreNavHostTest"
.\gradlew.bat :app:compileTmdbDebugAndroidTestKotlin
.\gradlew.bat :app:compileClientBDebugAndroidTestKotlin
.\gradlew.bat verifyDesignTokensLogFiles --console=plain
.\gradlew.bat check -PverifyDesignTokensLogFiles=true --continue --console=plain
```

## Required Device Journeys

- Search query -> focus first result -> Details -> Back -> verify same result focused and drawer closed.
- Home shelf card -> Details -> Back -> same card focused.
- Library item -> Details -> Back -> same item focused.
- Recommendation chain -> Back -> expected owning item/surface focus.
- Open drawer -> move Home/Search/Library -> Back and Right close paths -> verify prior content focus restoration.

## Acceptance Criteria

- Every tested Details/Player Back path restores the exact originating item when it still exists.
- Missing/removed targets use a documented stable fallback without crashes or focus loss.
- Drawer does not spuriously open on content Back.
- D-pad order, selected styling, scroll state, and query state remain stable.
- Root `check`, lint, verifier, both provider graphs, and all baseline tests remain green.

## Handoff Checklist

- [ ] Root cause and focus ownership/timing documented.
- [ ] Before/after layout evidence identifies the focused nodes.
- [ ] Home/Search/Library/drawer cases covered.
- [ ] Test counts and verifier delta reported.
- [ ] No unrelated navigation redesign.
