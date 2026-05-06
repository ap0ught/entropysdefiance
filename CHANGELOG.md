# Changelog

All notable changes to Entropy's Defiance are documented here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and the project adheres
to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [2.0.0] — 2026-05-06

First F-Droid-ready release. Application id changes from
`com.example.entropysdefiance` to `com.hwanderer.entropysdefiance`, so this is a
fresh install rather than an upgrade — the previous APK and the new one can
coexist on the same device until the older one is uninstalled.

### Added
- Interactive onboarding tutorial that walks through the FAB, task creation,
  swipe-to-move, Zen mode, calendar shortcut, app drawer, search and settings.
  Replayable from Settings → Introduction.
- Bundled Poppins font under SIL Open Font License — no longer fetched at runtime
  via Google Play Services.
- "Support development" entry in Settings linking to
  https://buymeacoffee.com/hwanderer.

### Changed
- Bundled font replaces Downloadable Fonts to remove the runtime dependency on
  Google Play Services.
- Release signing config no longer fails when keystore env vars are absent —
  required for reproducible F-Droid builds.
- Dead `READ_EXTERNAL_STORAGE` / `WRITE_EXTERNAL_STORAGE` permissions removed
  (had `maxSdkVersion="32"` while `minSdk=33` — they were never effective).

### Removed
- Empty double-tap stub on app drawer items (single-tap and double-tap behaved
  identically; the second handler was a placeholder).
