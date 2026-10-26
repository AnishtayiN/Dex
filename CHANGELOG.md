# Changelog
## Unreleased
- Minimum Android lowered from 8.0 (API 26) to 7.0 (API 24). The only
  API 26-only usage, `ApplicationInfo.category` for game detection, is
  now guarded, so on Android 7 games are not flagged and the games-only
  filter cannot identify them; search should be used there instead.
## 1.0.0 — 2026-10-26
- Initial DexBoost release: Shizuku connection/permission handling, installed-app
  browser with games filter and multi-select, ART compilation (verify,
  speed-profile, speed, reset) via `cmd package compile`, sequential cancellable
  queue, truthful results and local operation history, Persian/English
  localization, dark/light/system themes, DataStore settings.
- Performance improvements are not guaranteed; results report only what the
  command actually returned.
