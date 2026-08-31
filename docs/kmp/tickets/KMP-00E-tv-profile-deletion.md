# KMP-00E — Restore TV Profile Deletion Parity

## Goal

Make eligible profiles deletable with D-pad-only interaction on Android TV while preserving deterministic focus and confirmation behavior.

## Context

TV profile selection, creation, and editing pass. KMP-00 found deletion inaccessible: `TvProfilesScreen` can render the existing confirmation dialog,
but no TV control dispatches `ProfilesAction.RequestDeleteProfile`, and the TV editor exposes no delete action.

## Dependencies and Parallelization

- **Depends on:** KMP-00B.
- **Blocks:** KMP-00G and final KMP-00 acceptance.
- **Parallelization:** Yes, with KMP-00C, KMP-00D, and KMP-00F. This ticket exclusively owns common/TV profile edit-delete interaction and its
  focus restoration.

## In Scope

- A visible, focusable Delete action for eligible TV profiles.
- Existing request/confirm/dismiss state and use-case wiring.
- D-pad order, confirmation focus, cancellation, success focus restoration, and delete failure presentation.
- TV-specific preview/test coverage.

## Non-Goals

- No mobile/tablet profile redesign or repository/provider change.
- No hidden long-press-only delete gesture.
- Do not move auth/logout ownership into profile ViewModels.

## Implementation Tasks

1. Add a TV-specific Delete control to the edit surface when `profile.canDelete` is true; hide it for protected profiles.
2. Dispatch `ProfileEditorAction.RequestDeleteProfile` or `ProfilesAction.RequestDeleteProfile` through the existing ViewModel contract; do not call the
   repository from a composable.
3. Reuse the existing confirmation state/effect and present a TV-readable confirmation dialog with app-owned TV buttons.
4. Define deterministic focus order: editor fields -> Cancel -> Save -> Delete. Initial editor focus remains the display-name field.
5. Confirmation opens with Cancel focused, Right moves to Delete, Back/cancel restores focus to the Delete control, and confirmation blocks background
   interaction.
6. On successful deletion, return to profile selection and focus the nearest surviving profile; fall back to Add profile when none remain.
7. On failure, keep the profile/editor state, restore Delete focus, and show the standard error.
8. Add previews for deletable/non-deletable, confirmation, saving, and error-adjacent states.
9. Add TV Compose tests for visibility, actions, focus order/restoration, success, protected-profile behavior, and failure.

## Public API or Type Changes

- TV editor/screen APIs may gain platform-specific delete callbacks or focus requesters.
- Existing common profile actions/effects/use cases remain authoritative; no provider API change.

## Verification Commands

```powershell
.\gradlew.bat :feature:profiles:ui-common:compileDebugKotlin
.\gradlew.bat :feature:profiles:ui-tv:compileDebugKotlin
.\gradlew.bat :feature:profiles:ui-tv:compileDebugAndroidTestKotlin
.\gradlew.bat :feature:profiles:ui-mobile:compileDebugAndroidTestKotlin
.\gradlew.bat :feature:profiles:ui-tablet:compileDebugAndroidTestKotlin
.\gradlew.bat :app:compileTmdbDebugKotlin
.\gradlew.bat :app:compileClientBDebugKotlin
.\gradlew.bat verifyDesignTokensLogFiles --console=plain
.\gradlew.bat check -PverifyDesignTokensLogFiles=true --continue --console=plain
```

## Required Device Journeys

- TV: create temporary profile -> edit -> Delete -> cancel -> verify focus return -> Delete -> confirm -> verify removal and nearest-item focus.
- Verify protected primary profile has no Delete action.
- Repeat with ClientB and TMDB graphs and exercise Back at editor/confirmation/selection boundaries.

## Acceptance Criteria

- Every eligible TV profile can be deleted without touch/mouse input.
- Protected profiles cannot dispatch delete.
- D-pad order and confirmation/return focus are deterministic and visibly styled.
- Profile CRUD passes on mobile/tablet/TV for both providers.
- Root `check`, lint, verifier, and baseline tests remain green.

## Handoff Checklist

- [ ] TV Delete UI/action ownership documented.
- [ ] Protected/eligible behavior covered by tests.
- [ ] D-pad/focus screenshots and commands included.
- [ ] Test counts and verifier delta reported.
- [ ] No mobile/tablet behavior regression.
