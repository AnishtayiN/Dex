# Contributing to DexBoost
- Kotlin, Compose Material 3, coroutines/Flow, no Hilt (manual AppContainer in
  DexBoostApplication) to keep the build small and auditable.
- Keep UI, business logic, Shizuku communication and command construction
  separated (see README architecture). Never add a feature that fabricates a
  result: every reported outcome must come from a real process exit code/output.
- Shell commands: only predefined argv lists built by ArtCommandBuilder from
  validated package names. Never concatenate user text into a command string.
- Tests: run `./gradlew testDebugUnitTest` before submitting; add unit tests
  for any new command mapping, validation or result-parsing logic.
- No secrets, tokens or signing keys in the repository, ever.
- Localization: add every new visible string to both values/strings.xml and
  values-fa/strings.xml in the same change.
