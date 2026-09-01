# KMP-03/04 integration cleanup evidence

## Identity and scope

- Branch: `codex/kmp-03-04-integration-cleanup`.
- Base: `b8a1b43e0e7dbf400ca236a13e964d968441ffe1`.
- Verification date: 2026-09-01 (Europe/Athens).
- Target architecture: backend-agnostic Android-only KMP.
- Scope: remove the temporary JVM-to-Android-KMP compatibility bridge after all six consumers migrated, correct inherited KMP-00/KMP-02 test
  arithmetic, and refresh only stale operational README references.

No production Kotlin, resource, dependency, version, convention-plugin, module, SDK declaration, target, build type, flavor, generated output, or
provider implementation changed.

## Exact integration diff

- `build.gradle.kts`: removed the complete six-consumer/four-configuration transitional bridge and its now-unused
  `ArtifactTypeDefinition`, `TargetJvmEnvironment`, and `KotlinPlatformType` imports. All other root guards and subproject conventions remain.
- `docs/kmp/migration-baseline.md`: removed the nonexistent `:feature:player:ui-tablet` one-test row, corrected the accepted total from 215 to 214,
  and documented why stale generated XML was not executed evidence.
- `docs/kmp/KMP-02-evidence.md`: preserved the originally reported 234-file directory inventory while identifying its one stale ignored XML test;
  corrected the clean included-module total to 233 and the accepted base to 214. KMP-01 remains +4 and KMP-02 remains +15.
- `README.md`: changed Hilt/KSP to Koin 4.2.2 constructor injection, the Android SDK requirement to compile SDK 37 while retaining target SDK 36,
  and the focused migrated feature command from JVM `test` to `testAndroidHostTest`.
- `docs/kmp/KMP-03-04-integration-cleanup-evidence.md`: this verification record.

The corrected evidence progression is:

| Evidence point | Arithmetic | Tests |
|---|---:|---:|
| Accepted KMP-00 | corrected included-module baseline | 214 |
| KMP-01 | 214 + 4 | 218 |
| Clean KMP-02 | 218 + 15 | 233 |
| KMP-03 | 233 + 14 | 247 |
| KMP-04 | 247 + 7 | 254 |

KMP-03's 247 and KMP-04's 254 executed totals were already correct and were not rewritten.

## Static bridge proof

```powershell
rg -n "transitionalJvmCore|transitionalJvmCoreConsumerConfigurations|KotlinPlatformType|TargetJvmEnvironment|ArtifactTypeDefinition" build.gradle.kts
```

Result: no matches. The compatibility-only root declarations and imports are gone. The similarly named attributes in the standalone dependency
fixture remain intentional compatibility-matrix inputs and are unrelated to the deleted consumer bridge.

```powershell
rg -n "org.jetbrains.kotlin.jvm|streamcore.kmp.(library|compose.library)" `
  feature/login/domain/build.gradle.kts feature/profiles/domain/build.gradle.kts `
  feature/home/domain/build.gradle.kts feature/search/domain/build.gradle.kts `
  feature/details/data/build.gradle.kts feature/details/domain/build.gradle.kts
```

Result: all six former transitional consumers apply `streamcore.kmp.library`; none applies `org.jetbrains.kotlin.jvm`.

## KMP host tests

```powershell
.\gradlew.bat testAndroidHostTest --no-parallel `
  '-Dorg.gradle.jvmargs=-Xmx4096m -Dfile.encoding=UTF-8' --console=plain
```

Result: `BUILD SUCCESSFUL` in 29s; 117 actionable tasks (83 executed, 34 from cache). The generated host-test inventory contains 15 XML suites across
11 modules and 58 tests, with zero failures, errors, or skips. No expected host-test task executed zero tests.

## Provider compile graphs

```powershell
.\gradlew.bat :app:compileTmdbDebugKotlin :app:compileClientBDebugKotlin --no-parallel `
  '-Dorg.gradle.jvmargs=-Xmx4096m -Dfile.encoding=UTF-8' --console=plain
```

Result: `BUILD SUCCESSFUL` in 22s; 376 actionable tasks (71 executed, 269 from cache, 36 up-to-date). Both provider graphs resolve and compile without
the temporary bridge.

## Full root gate

```powershell
.\gradlew.bat check -PverifyDesignTokensLogFiles=true --continue --no-parallel `
  '-Dorg.gradle.jvmargs=-Xmx4096m -Dfile.encoding=UTF-8' --console=plain
```

Result: `BUILD SUCCESSFUL` in 1m 38s; 1,977 actionable tasks (1,152 executed, 401 from cache, 424 up-to-date).

- `verifyKmpTestTargets`: 11 common-test modules and 11 Android host-test targets; `:core:domain` is the compile-only exemption.
- `verifyKmpAndroidCompilerFlags`: one Compose-KMP Android compile task verified.
- `verifyKmpConventionPlugins`: two convention-only fixture modules verified.
- `verifyKmpDependencyCompatibility`: 14 locked coordinates resolved for common metadata and Android (14/12 artifact files).
- `verifyDesignTokens`: 300 production Kotlin files checked; zero violations. A focused `--rerun-tasks` invocation reproduced the exact count.
- Generated root test inventory: 59 XML suites, 254 tests, zero failures, errors, or skips.

## Scope and repository status

`git diff --check` passes. Static scans find no deleted bridge symbols/imports in the root build and no JVM plugin in the six former consumers.
The final diff is limited to the five files listed above; no unrelated file is modified. Generated build outputs remain ignored. The evidence commit
and clean post-commit status are reported in the final handoff.
