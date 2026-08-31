# KMP-00B — Complete Android Authentication and Session Lifecycle

## Goal

Add a production logout path on mobile, tablet, and TV so KMP-00 can verify login, logout, and restored-session behavior for both providers.

## Context

KMP-00 host verification is green at `8bc11dc4300e55822bbae689dfdad271ef3fe769`, but device acceptance found no production logout action or UI
call site. Provider repositories already implement `logoutUser`; the missing contract is app-owned UDF, navigation reset, and platform UI entry points.

## Dependencies and Parallelization

- **Depends on:** KMP-00A/remediation commit `8bc11dc4300e55822bbae689dfdad271ef3fe769`.
- **Blocks:** KMP-00C and final KMP-00 acceptance.
- **Parallelization:** No. This ticket owns app auth state, root navigation, and profile-surface integration.

## In Scope

- App-owned logout action/state/effect handling.
- A consistent Sign out entry point from profile selection on mobile, tablet, and TV.
- Confirmation, loading, failure, navigation reset, and active-profile cleanup.
- TMDB and ClientB logout/session-restoration tests and device journeys.

## Non-Goals

- No account deletion, registration, password reset redesign, token refresh redesign, KMP, Koin, or provider API changes.
- Do not move auth ownership into `:feature:profiles` or expose provider session types.
- Do not log, screenshot, or commit credentials/session values.

## Implementation Tasks

1. Add `AppAuthAction` in its own file and route all new auth UI events through `AppAuthViewModel.onAction`.
2. Extend immutable app auth UI state with logout confirmation/progress state without exposing repository or provider models.
3. On confirmation, call `AuthenticateRepository.logoutUser()` from `viewModelScope`; ignore duplicate submissions while in progress.
4. On success, clear the active profile, close confirmation state, and let `AuthStateModel.LoggedOut` drive a single navigation reset to Login with
   authenticated destinations removed from the back stack.
5. On failure, keep the session/profile intact, clear progress, and emit the existing app error presentation path.
6. Add an explicit secondary **Sign out** action to profile-selection surfaces only:
   - mobile: header action alongside Manage;
   - tablet: left action panel below Add profile;
   - TV: header action with deterministic D-pad order before Manage.
7. Render one app-owned confirmation dialog so feature ViewModels remain profile-only. Cancel must restore the invoking control's focus on TV.
8. Preserve empty production Login defaults and the existing invalid-submit test.
9. Add focused unit tests for confirmation, duplicate-submit protection, success, failure, active-profile cleanup, and back-stack reset.
10. Add/extend Compose tests for Sign out visibility, confirmation, loading disablement, cancellation, and TV focus restoration.

## Public API or Type Changes

- New app-owned `AppAuthAction` sealed interface.
- `AppAuthUiState.Ready` gains immutable logout confirmation/progress fields.
- Profile route/screen APIs gain app-owned `onLogoutRequested` callbacks; profile domain/ViewModels do not gain auth dependencies.

## Verification Commands

```powershell
.\gradlew.bat :app:testTmdbDebugUnitTest --tests "*AppAuthViewModelTest"
.\gradlew.bat :app:testClientBDebugUnitTest --tests "*AppAuthViewModelTest"
.\gradlew.bat :client:tmdb:data:testDebugUnitTest --tests "*TmdbAuthenticateRepositoryTest"
.\gradlew.bat :client:clientB:data:testDebugUnitTest --tests "*ClientBAuthenticateRepositoryTest"
.\gradlew.bat :feature:login:ui-common:testDebugUnitTest --tests "*LoginViewModelTest"
.\gradlew.bat :feature:profiles:ui-mobile:compileDebugAndroidTestKotlin
.\gradlew.bat :feature:profiles:ui-tablet:compileDebugAndroidTestKotlin
.\gradlew.bat :feature:profiles:ui-tv:compileDebugAndroidTestKotlin
.\gradlew.bat :app:compileTmdbDebugAndroidTestKotlin
.\gradlew.bat :app:compileClientBDebugAndroidTestKotlin
.\gradlew.bat :app:compileTmdbDebugKotlin
.\gradlew.bat :app:compileClientBDebugKotlin
.\gradlew.bat check -PverifyDesignTokensLogFiles=true --continue --console=plain
```

## Required Device Journeys

- TMDB phone: authenticated start -> Sign out -> confirmation cancel/focus return -> Sign out/confirm -> Login -> credential login -> restored-session
  relaunch.
- Tablet/TV: use the approved opaque TMDB session transfer when direct credentials are unavailable; verify Sign out clears it and Login is shown.
- ClientB phone/tablet/TV: generated non-secret login -> Sign out -> Login -> login again.
- TV: verify D-pad order, disabled/loading state, confirmation focus, cancel focus return, and Back behavior.

## Acceptance Criteria

- Every platform has an accessible, focusable Sign out action and confirmation.
- Both providers transition to `LoggedOut`, clear active profile state, and cannot return to authenticated routes with Back.
- Logout failure preserves the authenticated session and displays an error.
- Cold/restored session and logout/login journeys pass on phone/tablet/TV under the approved credential-handling contract.
- Root `check`, lint, verifier, and all baseline tests remain green with no count regression.

## Handoff Checklist

- [ ] Changed files grouped by app/profiles platform module.
- [ ] Auth state/action API changes documented.
- [ ] TMDB and ClientB logout/session results included.
- [ ] TV focus order and restoration evidence included.
- [ ] Test counts compared with the 185-test baseline.
- [ ] No credentials or provider session values printed or committed.
- [ ] No unrelated files modified.
