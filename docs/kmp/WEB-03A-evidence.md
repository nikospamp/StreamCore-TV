# WEB-03A Evidence — Browse Contracts and Integration Shell

## Dispatch

- Branch: `codex/web-03a-contract-shell`.
- Immutable base: `eb5467a661fed227f80f70bd3f8a2e91c52bf102` (accepted WEB-02 merge).
- Scope: integration-owner paths only. The four feature `ui-web` modules contain build shells and no `src/**` implementation.
- Credentials/live provider: not used.
- Target architecture: backend-agnostic; no provider DTO, SDK, response, or client model enters a core or feature UI contract.

## Frozen Public Contract

The public cross-module values are:

```kotlin
enum class WebBrowseDestination(val historyKey: String) {
    Home("home"),
    Search("search"),
    Library("library"),
    Details("details"),
}

@Immutable
data class WebBrowseFocusKey(
    val destination: WebBrowseDestination,
    val sectionKey: String,
    val itemKey: String,
)
```

`sectionKey` and `itemKey` reject blank values. Browser history stores only the versioned destination/section/item value; it never stores a
`ContentModel`, playback request, provider object, DOM node, or `FocusRequester`.

The app-owned shared UI surface is frozen as:

```kotlin
@Composable
fun StreamCoreWebTopChrome(
    activeDestination: WebBrowseDestination?,
    profileName: String,
    logoutInProgress: Boolean,
    onDestinationSelected: (WebBrowseDestination) -> Unit,
    onChangeProfile: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
)

@Composable
fun StreamCoreWebBrowseScaffold(
    activeDestination: WebBrowseDestination?,
    profileName: String,
    logoutInProgress: Boolean,
    onDestinationSelected: (WebBrowseDestination) -> Unit,
    onChangeProfile: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
)

@Composable
fun StreamCoreWebArtwork(
    imageUrl: String?,
    contentDescription: String?,
    fallbackText: String,
    requestWidthPx: Int,
    requestHeightPx: Int,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    containerColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    overlay: @Composable BoxScope.() -> Unit = {},
)
```

`StreamCoreWebArtwork` requires positive decode bounds. Existing app-owned web buttons/content cards are retained; their Space-key adapter now
honors disabled/loading state.

## Frozen Feature Entry Points

WEB-03B through WEB-03E implement these signatures in their own `commonMain` source trees. The selection callback carries the focus key so the app
shell can replace the origin history entry before pushing a child route.

```kotlin
@Composable
fun WebHomeRoute(
    profileId: String,
    selectedContentKey: WebBrowseFocusKey?,
    onContentSelected: (ContentModel, WebBrowseFocusKey) -> Unit,
    onError: (AppError) -> Unit,
    returnFocusKey: WebBrowseFocusKey?,
    onReturnFocusConsumed: (WebBrowseFocusKey) -> Unit,
    viewModel: HomeViewModel = koinViewModel(),
)

@Composable
fun WebHomeScreen(
    state: HomeUiState,
    onAction: (HomeAction) -> Unit,
    selectedContentKey: WebBrowseFocusKey?,
    returnFocusKey: WebBrowseFocusKey?,
    onReturnFocusConsumed: (WebBrowseFocusKey) -> Unit,
    modifier: Modifier = Modifier,
)
```

```kotlin
@Composable
fun WebSearchRoute(
    profileId: String,
    selectedContentKey: WebBrowseFocusKey?,
    onContentSelected: (ContentModel, WebBrowseFocusKey) -> Unit,
    onBack: () -> Unit,
    returnFocusKey: WebBrowseFocusKey?,
    onReturnFocusConsumed: (WebBrowseFocusKey) -> Unit,
    viewModel: SearchViewModel = koinViewModel(),
)

@Composable
fun WebSearchScreen(
    state: SearchUiState,
    onAction: (SearchAction) -> Unit,
    selectedContentKey: WebBrowseFocusKey?,
    returnFocusKey: WebBrowseFocusKey?,
    onReturnFocusConsumed: (WebBrowseFocusKey) -> Unit,
    modifier: Modifier = Modifier,
)
```

Search owns this platform adapter; its Wasm implementation passes the live DOM value to `onSubmitCommittedValue` before the screen dispatches
`QueryChanged(committedValue)` followed by `SubmitQuery`:

```kotlin
@Composable
internal expect fun WebSearchTextField(
    value: String,
    enabled: Boolean,
    requestFocus: Boolean,
    onValueChange: (String) -> Unit,
    onSubmitCommittedValue: (String) -> Unit,
    onEscape: () -> Unit,
    onFocusRequestConsumed: () -> Unit,
    modifier: Modifier = Modifier,
)
```

```kotlin
@Composable
fun WebLibraryRoute(
    profileId: String,
    selectedContentKey: WebBrowseFocusKey?,
    onContentSelected: (ContentModel, WebBrowseFocusKey) -> Unit,
    onError: (AppError) -> Unit,
    returnFocusKey: WebBrowseFocusKey?,
    onReturnFocusConsumed: (WebBrowseFocusKey) -> Unit,
    viewModel: LibraryViewModel = koinViewModel(key = "web-library:$profileId"),
)

@Composable
fun WebLibraryScreen(
    state: LibraryUiState,
    onAction: (LibraryAction) -> Unit,
    selectedContentKey: WebBrowseFocusKey?,
    returnFocusKey: WebBrowseFocusKey?,
    onReturnFocusConsumed: (WebBrowseFocusKey) -> Unit,
    modifier: Modifier = Modifier,
)
```

The profile-keyed Library ViewModel is mandatory: it prevents a failed profile switch from exposing the prior profile's retained lists without
changing the shared ViewModel contract.

```kotlin
@Composable
fun WebDetailsRoute(
    profileId: String,
    contentId: String,
    onRecommendationSelected: (ContentModel, WebBrowseFocusKey) -> Unit,
    onPlaySelected: (PlaybackRequestModel, WebBrowseFocusKey) -> Unit,
    onBack: () -> Unit,
    onError: (AppError) -> Unit,
    initialContent: ContentModel? = null,
    returnFocusKey: WebBrowseFocusKey?,
    onReturnFocusConsumed: (WebBrowseFocusKey) -> Unit,
    viewModel: DetailsViewModel = koinViewModel(),
)

@Composable
fun WebDetailsScreen(
    state: DetailsUiState,
    onAction: (DetailsAction) -> Unit,
    returnFocusKey: WebBrowseFocusKey?,
    onReturnFocusConsumed: (WebBrowseFocusKey) -> Unit,
    modifier: Modifier = Modifier,
)
```

## Frozen Navigation and Fixture Behavior

- Product paths: `/home`, `/search`, `/library`, `/details/{contentId}`, and the explicit non-playing `/player/{contentId}` placeholder.
- Diagnostic paths are isolated at `/diagnostic`, `/diagnostic/details/{contentId}`, and `/diagnostic/player/{contentId}`.
- Login/profile routes from WEB-02 remain intact. `/authenticated` is retained only as a legacy input and canonicalizes to Profiles or Home.
- Logged-out product paths canonicalize to Login. Authenticated protected paths without a selected profile canonicalize to Profiles before content
  is rendered. With a valid selected profile, direct Home/Search/Library/Details/Player paths survive reload.
- Successful logout clears the in-memory and account-scoped selected-profile value and replaces the route with Login. Failed provider logout
  preserves the current protected route and selection and returns the error.
- `captureReturnFocus` accepts a key only when its destination matches the current browse route. `consumeReturnFocus` requires whole-key equality and
  clears the value from the browser entry once.
- Fixture scenarios: Loading, Content, Empty, Offline, Error, LongText. Cross-feature fixture IDs are fixed in `WebBrowseFixtureIds`; features add
  their own backend-free models and stable test tags.

## Verification

All Gradle execution was serialized with `--max-workers=1` and used no live credentials.

| Command | Result |
|---|---|
| `:core:ui-web:compileKotlinWasmJs` | PASS; 18 actionable tasks (6 executed, 12 up-to-date) on the accepted retry |
| Four `:feature:*:ui-web:compileKotlinWasmJs` tasks | PASS; 77 actionable tasks (60 executed, 17 up-to-date) |
| `:webApp:compileKotlinWasmJs` | PASS; 128 actionable tasks (55 executed, 73 up-to-date) on the accepted retry |
| Focused `:webApp:wasmJsBrowserTest` | PASS in Chromium; 30 tests, 0 failures/errors/skips; final rerun 304 actionable tasks (62 executed, 242 up-to-date) |

Focused browser count:

- `WebRouteTest`: 6.
- `WebProductCoordinatorTest`: 23.
- `WebBrowseShellTest`: 1.

The existing Coil/Compose Skiko version-mismatch warning remains visible and is not introduced by WEB-03A. No Binaryen production distribution,
full browser matrix, Android/root gate, visual-approval claim, or live-provider run was performed for this contract checkpoint.

## Review

PASS. The single read-only severity-filtered acceptance review found no reproducible P0/P1, deterministic security/data-loss/accessibility
failure, backend-boundary violation, or explicit ticket acceptance failure. It confirmed protected Details/Player routing, direct-link preservation,
versioned value-only history state, exact-key focus consumption, logout behavior, chrome semantics/focus, four empty module shells, and the 30-test
post-source report. Non-blocking observations were limited to the intentionally retained `/authenticated` compatibility input, WEB-03C-owned native
search input implementation, and the expected absence of B–E production source in the module shells.
