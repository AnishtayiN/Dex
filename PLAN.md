# DexBoost implementation plan (Phase 1)
- Target device: Poco X3 Pro, Android 12 / MIUI, no root, Shizuku via wireless debugging/ADB.
- minSdk 26, targetSdk 31 (Android 12), compileSdk 34. compileSdk 34 > targetSdk is deliberate:
  building against 34 APIs while behaviour-targeting 31 keeps the app testable on the
  target device and avoids silently lowering the SDK. Behaviour changes of 32-34 are
  NOT opted into because targetSdk stays 31; check on Android 13/14 devices before
  raising targetSdk (ART Service replaces PackageManager dexopt in 14, see README).
- Shizuku 13.1.5 (dev.rikka.shizuku:api + :provider), ShizukuProvider in manifest.
  Execution: Shizuku.newProcess() is still the supported shell path in 13.x for running
  a shell command as the Shizuku server; the app ALSO binds a UserService (see
  shizuku/PrivilegedCommandExecutor.kt) so a future migration off newProcess is a
  one-file change. The executor interface is mockable; no code outside it touches
  Shizuku process APIs directly. Documented in README § Shizuku.
- ART command: `cmd package compile -m <filter> -f <package>` (PackageManager shell,
  Android 8-13). Reset: `cmd package compile --reset <package>`. On Android 14+ ART
  Service may reject/redirect these; a non-zero exit is reported truthfully, never
  remapped to success. Filters: verify, speed-profile, speed (quicken omitted: it is
  Android 11-or-lower only and adds no value over verify/speed-profile on API 31).
- Compatibility risks: (1) MIUI package visibility / background restrictions;
  (2) Shizuku must be re-activated after reboot (wireless debugging); (3) some OEM
  builds restrict `cmd package compile` for system apps; (4) compile on API 31 needs
  `-f` (force) or it may be skipped as already compiled — the app always passes -f
  and reports the real exit code/output; (5) games detection uses
  ApplicationInfo.category == CATEGORY_GAME (API 26+) plus a small keyword fallback
  only for labelling, never for shell decisions.
- Phases: 1 plan (this file) -> 2 foundation -> 3 Shizuku -> 4 core -> 5 UI polish
  -> 6 QA (testDebugUnitTest, assembleDebug, lintDebug).

## Verification record (2026-10-26, coordinator integration)
- Gradle in this VM is BLOCKED by the sandbox (direct TCP for Java
  denied; daemon loopback fails with type tag 109 = first byte of
  the sandbox refusal). JDK 17 (Temurin, ~/opt/jdk17) and Android SDK
  (platforms 31+34, build-tools 34.0.0, cmdline-tools, ~/Android/Sdk)
  were installed from official downloads; Gradle 8.7 was installed
  directly (~/opt/gradle-8.7) because the wrapper's Java download
  also fails through the proxy. ./gradlew commands are expected to
  work on a normal machine / in CI — see README.
- Instead verified: kotlinc frontend type-check of ALL main sources
  against real dependency jars + android-34 (R stub for 158 strings);
  only Compose bytecode generation remains, which needs the Gradle
  Compose plugin (already declared). JUnit 4 run of all unit tests:
  43/43 OK (see app/src/test). XML well-formed; fa/en keys 158=158.
- Integration fixes applied by the coordinator (would have failed
  Gradle too): Shizuku executor rewritten from private newProcess
  to a UserService (PrivilegedCommandService + Binder transact,
  allow-list validated in-service); QueueScreen bad QueueStatus
  import removed; @file:OptIn(ExperimentalMaterial3Api) added to the
  7 screens using TopAppBar; ArtCompilationManager callback default
  fixed; test suspend-lambda fixed; MainActivity
  onRequestPermissionsResult signature fixed (Array<String>).
- NOT yet verified (needs a device + working Gradle): assembleDebug
  APK, lintDebug, Room KSP generation at build time, Shizuku
  UserService binding on the Poco X3 Pro, actual `cmd package
  compile` behaviour on MIUI Android 12, and the full UI on device.
  These are the first checks for whoever runs the project next.
