# Changelog

All notable changes to Entropy's Defiance are documented here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and the project adheres
to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [3.1.0] — 2026-06-17

### Added
- Default-launcher prompt after onboarding, plus a "Default launcher" entry in
  Permissions — for OEMs (e.g. Samsung One UI) that never ask to switch home apps.
- Accent colour options: follow the system Material You palette, choose a preset,
  or enter a custom hex colour.
- Left-handed mode (a top-level Settings toggle) that mirrors the floating button,
  the app-drawer swipe zone, the app-list alignment, and the calendar to the left.

### Changed
- Redesigned the calendar agenda: real dates on every row, correct multi-day spans,
  a progress bar on ongoing events, 12/24-hour times following the device setting,
  and ongoing events grouped under today (no stray day-separator).
- Refreshed task creation, the settings menu and sub-screens, and the alignment
  screen to a consistent, borderless typographic style.
- Expanded the introduction (peek, swipe-to-move tasks, gestures, file sync).
- Removed the non-functional dark-mode toggle.

### Fixed
- Tasks no longer vanish when the task file is momentarily unreadable or mid-sync:
  a failed/partial read is no longer treated as "no tasks", and an empty list is
  never written back over a failed read.
- App-drawer keyboard now reappears immediately when scrolling back to the top
  (previously a 3–4 s delay), and closing the drawer is instant.
- Removed wasteful per-app icon loading on the main thread (the launcher is
  text-only), improving cold-start/unlock responsiveness.

## [3.0.1] — 2026-06-09

### Fixed
- Tasks created on a fresh install no longer vanish. The app now requires a
  storage location immediately after the welcome, instead of silently dropping
  tasks when no task file was configured.
- The system back button now returns to the home screen from the task creation
  screen, and its back arrow is rendered with a clearly visible tint.

## [3.0.0] — 2026-05-06

### Changed
- Replaced step-by-step tutorial overlay with a full-screen welcome experience:
  six swipeable pages introducing the app's philosophy before the first use.
  Replayable from Settings → Introduction.

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
