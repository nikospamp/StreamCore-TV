# Feature Creation Template

This document defines the starter shape for new features in StreamCoreTV.

The target architecture is backend-agnostic: feature, core, domain, and UI modules must not depend on provider SDKs, DTOs, API responses, or client-specific models. Client modules own provider details and map them into shared app models or feature-local models.

Use `:feature:login` as the current reference slice, but do not blindly copy it. Generate only the areas the new feature needs.

## Feature Inputs

Define these before creating a feature:

```text
featureName = profile
platforms = mobile, tablet, tv
requiresRepository = true | false
repositoryScope = core-domain | none
requiresFeatureData = true | false
requiresClientData = true | false
clients = clientA, clientB
sharedModels = ProfileModel, SubscriptionModel
featureData = ProfileFilterModel, ProfileScreenParams
```

Guidance:

- Use `core:domain` for generic repository contracts. Client modules must implement these contracts.
- Use `core:model` for reusable app models, such as `ProfileModel`, that can be consumed by multiple features.
- Use `feature:<feature-name>:domain` for feature use cases and feature business rules.
- Use `feature:<feature-name>:data` only for feature-local data classes that should not become app-wide contracts.
- Use `requiresClientData = false` for presentation-only or orchestration features.

## Module Layout

Full feature module shape:

```text
feature/<feature-name>/
  data/
  domain/
  ui-common/
  ui-mobile/
  ui-tablet/
  ui-tv/
```

Only create `data/` when `requiresFeatureData = true`.
Only create platform UI modules requested by `platforms`.

### `feature/<feature>/data`

Owns feature-local data classes consumed by the feature domain, ViewModel, and UI state.

```text
feature/<feature>/data/
  build.gradle.kts
  src/main/kotlin/.../feature/<feature>/data/
    <Feature>FilterModel.kt
    <Feature>Params.kt
    <Feature>Result.kt
```

Rules:

- Kotlin JVM module unless Android APIs are required.
- Contains feature-local data classes.
- No reusable app models. Put reusable models in `:core:model`.
- No client DTOs, SDKs, API responses, or provider models.
- No repository implementations.
- No Android UI dependencies.
- Prefer immutable `data class` models.
- Avoid mutable collections.
- Use `Model` suffix for data consumed by domain, ViewModels, or composables.

### `feature/<feature>/domain`

Owns feature-specific business rules and use cases.

```text
feature/<feature>/domain/
  build.gradle.kts
  src/main/kotlin/.../feature/<feature>/domain/
    <Feature>UseCase.kt
  src/test/kotlin/.../feature/<feature>/domain/
    <Feature>UseCaseTest.kt
```

Rules:

- Kotlin JVM module unless Android APIs are required.
- Depends on `:core:domain` when use cases call repository contracts.
- Depends on `:core:model` when use cases consume reusable app models.
- Depends on `:feature:<feature>:data` only when use cases consume feature-local data classes.
- No Android UI dependencies.
- No client DTOs, SDKs, API responses, or provider models.
- No repository contract definitions.
- Use cases orchestrate `:core:domain` repository contracts, reusable app models, and feature-local data classes.
- Use `AppResult` / `AppError` for recoverable operation results.

### `feature/<feature>/ui-common`

Owns ViewModel, UDF contracts, shared presentation components, and shared UI tests.

```text
feature/<feature>/ui-common/
  build.gradle.kts
  src/main/kotlin/.../feature/<feature>/common/
    contract/
      <Feature>UiState.kt
      <Feature>Action.kt
      <Feature>Effect.kt
    effects/
      <Feature>RouteEventEffect.kt
    presentation/
      <Feature>ViewModel.kt
      shared composables...
    testing/
      <Feature>TestTags.kt
  src/test/kotlin/.../feature/<feature>/common/
    <Feature>ViewModelTest.kt
    MainDispatcherRule.kt
```

Rules:

- Route modules collect state; screen composables remain stateless.
- ViewModel exposes one immutable `StateFlow<<Feature>UiState>`.
- UI sends events upward through `<Feature>Action`.
- One-shot navigation/error events use `<Feature>Effect`.
- Effects are collected with lifecycle awareness.
- Keep expensive derivation out of composables.
- Use immutable collections in UI state.

Starter contract:

```kotlin
data class ProfileUiState(
    val isLoading: Boolean = false,
    val error: AppError? = null,
)

sealed interface ProfileAction {
    data object Refresh : ProfileAction
}

sealed interface ProfileEffect {
    data class ShowError(val error: AppError) : ProfileEffect
}
```

Starter ViewModel pattern:

```kotlin
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val loadProfile: LoadProfileUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val effectsChannel = Channel<ProfileEffect>(Channel.BUFFERED)
    val effects: Flow<ProfileEffect> = effectsChannel.receiveAsFlow()

    fun onAction(action: ProfileAction) {
        when (action) {
            ProfileAction.Refresh -> refresh()
        }
    }

    private fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = loadProfile()) {
                is AppResult.Success -> {
                    _uiState.update {
                        it.copy(isLoading = false)
                    }
                }
                is AppResult.Failure -> {
                    _uiState.update {
                        it.copy(isLoading = false, error = result.error)
                    }
                    effectsChannel.send(ProfileEffect.ShowError(result.error))
                }
            }
        }
    }
}
```

### `feature/<feature>/ui-mobile`

Owns mobile route and mobile screen.

```text
feature/<feature>/ui-mobile/
  build.gradle.kts
  src/main/kotlin/.../feature/<feature>/mobile/
    Mobile<Feature>Route.kt
    Mobile<Feature>Screen.kt
  src/androidTest/kotlin/.../feature/<feature>/mobile/
    Mobile<Feature>ScreenTest.kt
```

Rules:

- `Route` gets the ViewModel with Hilt and collects `uiState` using `collectAsStateWithLifecycle()`.
- `Screen` is stateless and preview-friendly.
- Use `@PreviewMobile`.
- Wrap previews in `StreamCoreTVTheme`.
- Prefer app design-system components.

### `feature/<feature>/ui-tablet`

Owns tablet route and adaptive tablet screen.

```text
feature/<feature>/ui-tablet/
  build.gradle.kts
  src/main/kotlin/.../feature/<feature>/tablet/
    Tablet<Feature>Route.kt
    Tablet<Feature>Screen.kt
  src/androidTest/kotlin/.../feature/<feature>/tablet/
    Tablet<Feature>ScreenTest.kt
```

Rules:

- Same route/screen split as mobile.
- Use `@PreviewTablet`.
- Use window size classes/adaptive layout decisions where needed.
- Do not use `isTablet` checks for layout behavior.

### `feature/<feature>/ui-tv`

Owns TV route and TV screen.

```text
feature/<feature>/ui-tv/
  build.gradle.kts
  src/main/kotlin/.../feature/<feature>/tv/
    Tv<Feature>Route.kt
    Tv<Feature>Screen.kt
  src/androidTest/kotlin/.../feature/<feature>/tv/
    Tv<Feature>ScreenTest.kt
```

Rules:

- Same route/screen split as mobile.
- Use `@PreviewTV`.
- Account for D-pad navigation, focus states, large spacing, and 10-foot readability.
- Use TV Material or app-owned TV design-system components where required.
- Add focused tests for default focus and critical D-pad behavior.

## Model Placement

Reusable app models live in the common model module. In this repo, that module is `:core:model`.

Example:

```text
core/model/src/main/kotlin/com/pampoukidis/streamcoretv/core/model/auth/ProfileModel.kt
```

Rules:

- Use `Model` suffix for objects consumed by domain, ViewModels, or composables.
- Do not use `Ui` or `UI` suffix for common app data.
- Prefer immutable `data class` models.
- Avoid mutable collections.
- Keep models backend-agnostic.
- Put models in `:core:model` when they can be used across features or app shell flows.

Example:

```kotlin
data class ProfileModel(
    val id: String,
    val displayName: String,
    val avatarUrl: String?,
)
```

Feature-local data classes live in `:feature:<feature>:data`.

Example:

```text
feature/profile/data/src/main/kotlin/com/pampoukidis/streamcoretv/feature/profile/data/ProfileFilterModel.kt
feature/profile/data/src/main/kotlin/com/pampoukidis/streamcoretv/feature/profile/data/ProfileScreenParams.kt
```

Use `:feature:<feature>:data` for data contracts owned by one feature and not intended to be reused across the app.

## Client Data

Client/provider modules own DTOs, SDK integrations, network clients, mappers, repository implementations, and Hilt bindings.

Example:

```text
data/clientA/src/main/kotlin/com/pampoukidis/streamcoretv/data/client/
  model/ProfileDto.kt
  mapper/ProfileMapper.kt
  profile/ClientAProfileRepository.kt
  di/ClientAProfileModule.kt
```

Rules:

- DTOs stay inside `data/client*`.
- DTOs should generally be `internal`.
- Mappers convert provider representations into `:core:model` for reusable app models.
- Mappers convert provider representations into `:feature:<feature>:data` only for feature-local models.
- Feature UI never imports client DTOs, Room entities, preferences models, or SDK types.
- Repository implementations bind to `core:domain` repository interfaces.
- Client modules may depend on `:core:model` and, when needed, `:feature:<feature>:data`.

Allowed conversion flow:

```text
Dto -> core Model -> Db
Dto -> feature-local Model
Db -> core Model
core Model -> Dto
```

Example:

```kotlin
internal data class ProfileDto(
    val id: String,
    val name: String,
    val avatar: String?,
)

internal fun ProfileDto.toModel(): ProfileModel =
    ProfileModel(
        id = id,
        displayName = name,
        avatarUrl = avatar,
    )
```

## Repository Scope

Use `:core:domain` for generic repository contracts that are available across the app. Client modules implement these contracts.

Examples:

```text
AuthenticateRepository
ProfileRepository
PlaybackRepository
CatalogRepository
```

Decision rule:

```text
Repository contract?         -> core:domain
Reusable app model?          -> core:model
Use case / business rule?    -> feature:<name>:domain
Feature-local data class?    -> feature:<name>:data
Provider-specific details?   -> data:<client>
```

## Build File Defaults

### Data module

```kotlin
plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    jvmToolchain(11)
}
```

### Domain module

```kotlin
plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    jvmToolchain(11)
}

dependencies {
    implementation(projects.core.domain)
    implementation(projects.core.data)
    implementation(projects.feature.<feature>.data)
    implementation(libs.javax.inject)

    testImplementation(libs.junit)
}
```

Remove `implementation(projects.feature.<feature>.data)` only when the feature has no feature-local data module.

### UI common module

```kotlin
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.dagger.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.pampoukidis.streamcoretv.feature.<feature>.common"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        minSdk = 24
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    api(projects.feature.<feature>.domain)
    api(projects.feature.<feature>.data)

    implementation(projects.core.domain)
    implementation(projects.core.data)
    implementation(projects.core.ui)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.runtime)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.dagger.hilt.android)
    ksp(libs.dagger.hilt.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
```

Remove `api(projects.feature.<feature>.data)` only when the feature has no feature-local data module.

### Platform UI modules

```kotlin
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.pampoukidis.streamcoretv.feature.<feature>.<platform>"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        minSdk = 24
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    api(projects.feature.<feature>.uiCommon)

    implementation(projects.core.data)
    implementation(projects.core.ui)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
```

## Gradle Wiring

Add feature modules to `settings.gradle.kts`:

```kotlin
include(":feature:<feature>:domain")
include(":feature:<feature>:ui-common")
include(":feature:<feature>:ui-mobile")
include(":feature:<feature>:ui-tablet")
include(":feature:<feature>:ui-tv")
```

Add `include(":feature:<feature>:data")` only when the feature has feature-local data classes.

Add platform feature modules to `:app`:

```kotlin
implementation(projects.feature.<feature>.uiMobile)
implementation(projects.feature.<feature>.uiTablet)
implementation(projects.feature.<feature>.uiTv)
```

Add client implementations only to flavor-specific configurations:

```kotlin
clientAImplementation(projects.data.clientA)
clientBImplementation(projects.data.clientB)
```

## Navigation / App Shell Wiring

Feature modules should expose route composables, not own app-level navigation decisions.

The app shell decides which route to show and wires callbacks:

```kotlin
MobileProfileRoute(
    onBack = onBack,
    onOpenSettings = onOpenSettings,
    onError = onError,
)
```

Route rules:

- Collect state.
- Collect effects.
- Forward callbacks.
- Render the stateless screen.

Screen rules:

- Accept state and callbacks only.
- No ViewModel access.
- No repository/DI access.
- Preview with static sample state.

## Testing Checklist

Add focused tests for:

- Use cases.
- Mappers.
- Repository behavior.
- ViewModel state transitions.
- Flow behavior.
- Mobile/tablet/TV screen presence.
- TV default focus and critical D-pad navigation.

Add screenshot tests for important states when practical:

- Loading.
- Content.
- Empty.
- Offline.
- Error.
- Unauthenticated.
- Entitlement blocked.
- Long localized text.

## Performance Checklist

Before merging a new feature:

- `UiState` is immutable.
- No mutable collections in `UiState`.
- `Route` uses `collectAsStateWithLifecycle()`.
- Composables are stateless where possible.
- Expensive formatting/filtering/sorting is outside composables or memoized correctly.
- Lazy layouts have stable `key` and `contentType`.
- Effects use `rememberUpdatedState` for changing callbacks.
- Flow collection does not keep doing work in the background unnecessarily.
- Release or benchmark builds are used for performance claims.

## Memory Checklist

Before merging a new feature:

- No long-lived references to `Activity`, `Context`, or composables in ViewModel.
- Coroutines are scoped to `viewModelScope` or an injected lifecycle-safe scope.
- Flow sharing uses an explicit timeout where appropriate.
- Channels/flows do not retain large payloads longer than needed.
- Client SDK listeners are unregistered in repository/data layer ownership.

## Scaffold Output Checklist

A generated feature should create or update:

```text
settings.gradle.kts
app/build.gradle.kts
feature/<feature>/domain/build.gradle.kts
feature/<feature>/ui-common/build.gradle.kts
feature/<feature>/ui-mobile/build.gradle.kts
feature/<feature>/ui-tablet/build.gradle.kts
feature/<feature>/ui-tv/build.gradle.kts
feature/<feature>/.../<Feature>UiState.kt
feature/<feature>/.../<Feature>Action.kt
feature/<feature>/.../<Feature>Effect.kt
feature/<feature>/.../<Feature>ViewModel.kt
feature/<feature>/.../<Platform><Feature>Route.kt
feature/<feature>/.../<Platform><Feature>Screen.kt
feature/<feature>/.../<Feature>TestTags.kt
```

Optional output:

```text
core/model/.../<Entity>Model.kt
core/domain/.../<Capability>Repository.kt
feature/<feature>/data/build.gradle.kts
feature/<feature>/data/.../<FeatureLocal>Model.kt
data/clientA/.../<Entity>Dto.kt
data/clientA/.../<Entity>Mapper.kt
data/clientA/.../ClientA<Capability>Repository.kt
data/clientA/.../di/ClientA<Capability>Module.kt
data/clientB/.../<Entity>Dto.kt
data/clientB/.../<Entity>Mapper.kt
data/clientB/.../ClientB<Capability>Repository.kt
data/clientB/.../di/ClientB<Capability>Module.kt
```

## Proposed Generator Command

Future scaffold script shape:

```powershell
.\tools\new-feature.ps1 `
  -Name profile `
  -Platforms mobile,tablet,tv `
  -Repository core-domain `
  -Clients clientA,clientB `
  -SharedModels Profile,Subscription `
  -FeatureData ProfileFilter,ProfileScreenParams
```

The generator should be conservative:

- Do not create client data files unless requested.
- Do not create shared app models unless requested.
- Do not create feature-local data classes unless requested.
- Do not overwrite existing files.
- Print the exact files created and the Gradle files changed.
- Leave TODO markers only where provider-specific behavior is required.
