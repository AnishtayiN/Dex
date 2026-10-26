# DexBoost

DexBoost is an Android utility that manages Android Runtime (ART)
compilation for installed applications — especially games — using the
supported package-manager compilation commands, executed with
Shizuku's ADB/shell-level privilege. No root is required, and no root
is requested.

**Performance improvements are not guaranteed.** ART compilation
changes how an app's DEX code is compiled ahead of time; it does not
overclock a GPU, change thermal limits, lower network ping, or
guarantee higher FPS. DexBoost reports only what the compilation
command actually returned — exit code, output, and duration — and
never invents FPS, battery, temperature, RAM, or storage gains.

## Features

- **Shizuku connection and permission handling** — detects whether
  Shizuku is installed, running, and permitted; requests permission
  through the official API; survives binder death and permission
  revocation without crashing; shows step-by-step setup help,
  including that wireless-debugging activation must be repeated
  after a reboot.
- **Privileged execution via a UserService** — commands run in a
  Shizuku-started service (`PrivilegedCommandService`) that executes
  argv arrays directly (no shell). The service independently
  re-validates every command: only `cmd package compile` with an
  allowed filter or `--reset`, for a validated package name, is ever
  executed. In Shizuku API 13.1.5 the older `Shizuku.newProcess`
  path is private and is therefore *not* used.
- **Installed-app browser** — real `PackageManager` discovery with
  icon, name, package name, version, and install time; search, sort,
  games-only filter (via `ApplicationInfo.category`), system-app
  toggle, single and multi-select. DexBoost itself is always
  excluded. Package visibility limits on Android 11+ are documented
  below.
- **ART compilation modes** — `verify`, `speed-profile`, `speed`,
  and `reset`, built only from a typed enum in one place
  (`ArtCommandBuilder`). The command is
  `cmd package compile -m <filter> -f <package>` (reset:
  `cmd package compile --reset <package>`).
- **Profiles** — Balanced (`speed-profile`), Maximum AOT (`speed`,
  with a storage/time warning), and Reset (confirmation required).
  Profiles are presets, not magic performance modes.
- **Sequential, cancellable queue** — one app at a time, with the
  current package, completed/failed/remaining counts, and a
  confirmation screen before a batch (selected packages, mode, side
  effects — no invented time or storage estimates).
- **Truthful results and history** — package, mode, start/end,
  exit code, stdout/stderr, duration, and an outcome of confirmed,
  failed, unsupported, permission-denied, timeout (unknown outcome),
  cancelled, or unknown. An empty output is never treated as proof
  of success, and a timeout is never reported as success or failure.
  History is stored locally in Room and can be cleared.
- **UI** — dark-first Material 3 (deep charcoal, restrained green /
  cyan accents), all required screens: Home, Installed Apps, App
  Details, Optimization Queue, Results & History, Shizuku Setup,
  Settings.
- **Localization** — complete English and Persian (RTL) string
  resources (158 keys each). Package names and commands are always
  shown in Latin exactly as-is.
- **Settings** — language (English / Persian / system), theme
  (dark / light / system), default compilation mode, confirmation
  behaviour, history management, about / privacy / limitations.
- **Privacy** — no analytics, no accounts, no network use at all
  after installation. Installed-app lists and results never leave
  the device.

## Architecture

```
app/src/main/java/com/dexboost/
  MainActivity.kt, DexBoostApplication.kt   // manual singletons, no DI framework
  data/
    model/OperationRecord.kt                // Room entity
    repository/ (Dao, Database, HistoryRepository)
    preferences/UserPreferences.kt          // DataStore settings (+ sync mirror
                                            // for language/theme used pre-Compose)
  domain/model/ (CompilationMode, CommandResult, InstalledApp)
  shizuku/
    ShizukuManager.kt                       // binder + permission state
    ShizukuPermissionController.kt
    PrivilegedCommandExecutor.kt            // interface (mockable in tests)
    PrivilegedCommandService.kt             // UserService run by Shizuku
    ShizukuCommandExecutor.kt               // bindUserService + Binder transact
    ShizukuSetupGuide.kt
  optimization/
    ArtCommandBuilder.kt                    // the ONLY place commands are built
    PackageNameValidator.kt                 // validation + shell-arg escaping
    ArtCompilationManager.kt                // sequential queue, cancellation
  apps/ (InstalledAppsRepository, PackageManager impl)
  ui/ (navigation, screens, components, theme, LanguageHelper)
  viewmodel/ (Apps, Optimization, Settings, Shizuku, factory)
```

No giant `MainActivity`: it only wires ViewModels, applies the
language, and hosts the navigation graph.

## Requirements

- Android Studio (Hedgehog or newer) or the command line below.
- JDK 17 (required by AGP 8.5).
- Android SDK: compileSdk 34, build-tools 34.0.0, platform
  android-34 (android-31 is enough to run, but the build targets
  the compile SDK above).
- A device for real testing: Poco X3 Pro, Android 12 / MIUI, no
  root, Shizuku installed (see Shizuku setup). The app itself runs
  on Android 8.0 (API 26) and newer.

### SDK versions — deliberate choices

- `minSdk = 26` (Android 8.0), `targetSdk = 31` (Android 12),
  `compileSdk = 34`. Targeting 31 is deliberate: it is the primary
  test device's version, and it avoids opting into the behaviour
  changes of Android 13/14. In particular, Android 14 replaces
  PackageManager dexopt with ART Service, so `cmd package
  compile` may be rejected or redirected there — DexBoost reports
  that truthfully (non-zero exit) rather than hiding it. Do not
  raise `targetSdk` without re-testing compilation on Android 14+.
- Dependency versions are pinned in `gradle/libs.versions.toml`
  (AGP 8.5.2, Kotlin 2.0.21, Compose BOM 2024.09.00, Room 2.6.1,
  DataStore 1.1.1, Shizuku 13.1.5). Never silently lower a version
  to hide a build error.

## Build instructions

```sh
# 1. Point the build at your SDK (local.properties is git-ignored):
echo "sdk.dir=$HOME/Android/Sdk" > local.properties

# 2. Unit tests:
./gradlew testDebugUnitTest

# 3. Debug APK (output: app/build/outputs/apk/debug/app-debug.apk):
./gradlew assembleDebug

# 4. Lint:
./gradlew lintDebug
```

CI (`.github/workflows/android-ci.yml`) does the same on every
push/PR to `main` and uploads the debug APK only after a
successful build. No signing keys or secrets are in the repository.

### Known build-environment limitation (VM used for development)

In the development VM used to write this project, Gradle builds
could **not** run to completion: the sandbox blocks direct TCP
connections for Java processes ("Other TCP connections is turned
off"), which breaks the Gradle daemon's loopback protocol — the
client fails with `DaemonConnectionException: Could not receive a
message from the daemon` / `Unexpected type tag 109` (the first
byte of the sandbox's refusal message). This is an environment
restriction, not a project defect: the same `./gradlew` commands
above are expected to work on a normal workstation or in CI.
If you hit the same error in a sandbox, allow direct TCP
connections for the build tool (or run the build in CI) instead of
changing the project to work around it.

What *was* verified in that VM instead (honestly, not via Gradle):

- All main sources pass the Kotlin compiler's frontend
  (type-checking, symbol resolution) against the real dependency
  jars and the Android 34 SDK, using a generated `R` stub for the
  158 string resources (Gradle generates the real `R` class).
  The only remaining compiler step that needs Gradle is Compose
  bytecode generation: the standalone compiler cannot inline
  `remember` without the Compose compiler plugin that the Gradle
  build already applies (`org.jetbrains.kotlin.plugin.compose`).
- Unit tests for the engine, run directly with JUnit 4:
  **43/43 pass** (mode→command mapping, package validation, shell
  escaping, result interpretation, queue ordering/cancellation,
  concurrent-batch rejection, UserService command allow-list).
  Run them under Gradle with `./gradlew testDebugUnitTest` on a
  machine where Gradle works.
- XML well-formedness of every resource file; English/Persian
  string-key equality (158 = 158); no TODO/FIXME, no secrets, no
  hardcoded success states in the code.

Two real defects found by that verification and fixed in the
code (they would also have failed a Gradle build):

- `ArtCompilationManager.runBatch` defaulted a 3-parameter
  callback to `{}` (now `{ _, _, _ -> }`).
- A test lambda called a suspend function from a non-suspend
  function type (now a `suspend` function type).

## Shizuku setup (on the phone)

1. Install the Shizuku app.
2. Enable Developer options, then Wireless debugging (Android
   11+), or connect the phone to a PC and use ADB.
3. In Shizuku, tap **Start** and complete pairing (notification /
   pairing code).
4. Open DexBoost and grant Shizuku permission when prompted.
5. **After every reboot, wireless-debugging activation must be
   repeated** before Shizuku (and DexBoost) can work again.
6. If DexBoost reports "Shizuku is not ready", open the in-app
   **Shizuku Setup** screen: it shows the current state, the
   steps above, and troubleshooting for the common cases
   (not installed, not running, permission denied, binder died).

Shizuku gives ADB/shell-level privilege, not root. DexBoost never
asks for root and never claims to have it.

## Device testing steps (Poco X3 Pro, Android 12)

1. Install the debug APK, grant Shizuku permission as above.
2. Home screen should show Shizuku "Ready" and a non-zero
   discoverable-app count.
3. Open Installed Apps, search for a game, select it, choose
   Balanced, confirm, and watch the Queue screen: the item should
   move QUEUED → RUNNING → DONE with a real exit code and
   duration.
4. Check History: the record must match the queue result exactly.
5. Kill Shizuku mid-queue (or revoke permission) and confirm the
   app reports the interruption as cancelled/unknown, never as
   success, and does not crash.
6. Repeat with Maximum AOT on one app and Reset on one app;
   confirm the warning/confirmation dialogs appear.
7. Switch language to Persian in Settings and confirm RTL layout,
   correct Persian text, and Latin package names/commands.

## Supported and unsupported behaviour

Supported: ART compilation via `cmd package compile` on Android
8–13 (primary target Android 12); `verify`, `speed-profile`,
`speed`, and `--reset` where the device's package manager accepts
them; sequential cancellable batches; local history.

Unsupported / deliberately not done: GPU/CPU/thermal/kernel
changes; force-stopping apps without consent; rebooting the
device; disabling security features; modifying system partitions;
any general-purpose shell terminal; arbitrary user-typed shell
commands; any claim about FPS, ping, battery, or temperature.
On Android 14+, ART Service may reject the legacy command — that
is reported as a failure/unsupported result, not silently worked
around. On some MIUI builds, compilation of system apps may be
refused by the package manager; that is also reported as-is.
Package visibility (Android 11+): DexBoost declares narrow
`<queries>` for launcher apps plus `QUERY_ALL_PACKAGES` only up
to API 30; on API 31+ some installed packages may legitimately
not be visible to the app, and the Apps screen shows only what
the system allows it to see.

## Troubleshooting

| Symptom | Likely cause / fix |
|---|---|
| "Shizuku is not ready" | Shizuku not started, or reboot cleared wireless debugging — redo Shizuku setup. |
| Permission denied result | DexBoost's Shizuku permission was revoked — grant it again from the Setup screen. |
| Unsupported result | The device's package manager rejected the mode (e.g. Android 14 ART Service, or an OEM restriction) — try another mode; do not assume success. |
| Timeout result | Compilation took longer than 5 minutes for one app — the outcome is unknown; the app may or may not have been compiled. Re-running is safe. |
| App missing from the list | Package visibility (Android 11+) or a work profile the app cannot see — see above. |
| Gradle daemon error in a sandbox | See "Known build-environment limitation" above — allow direct TCP or use CI. |

## Privacy and security

- No analytics, no crash reporting, no accounts, no network
  requests. Everything (settings, history) stays in the app's
  private storage.
- Shell commands are argv lists built from a fixed enum and a
  validated package name, and re-validated inside the privileged
  service. User text is never concatenated into a command.
- Logs contain no package lists, no output dumps, and no
  credentials. There are no credentials in this repository at all.

## Changelog / contributing

See `CHANGELOG.md` and `CONTRIBUTING.md`. No license file is
included: the repository owner has not specified a license, so
none is invented here.
