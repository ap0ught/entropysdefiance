# Changelog

All notable changes to Entropy's Defiance are documented here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and the project adheres
to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [3.4.0] — 2026-09-01

### Added
- Zen Mode can enable Do Not Disturb for the duration of a session and restore the
  previous filter when it ends (Settings → Zen Mode). Opt-in, priority-only by
  default with total silence as a separate choice. A filter the user set themselves
  is never overwritten, and a filter left on by a process death is cleared on the
  next cold start.
- Hide and rename apps from the app drawer's long-press menu. Hidden entries are
  restored from Settings → Hidden apps; searching a renamed app by its installed
  name still finds it.
- Fullscreen option hiding the status bar (Settings → Appearance). The navigation
  bar is left alone so it cannot compete with the drawer's swipe-up.
- Adjustable wallpaper dim (Settings → Appearance), previously fixed at 60%.
- A shared motion system: screen changes now express the app's own sense of scope —
  Zen Mode collapses inward, Alignment steps back, settings push and pop by depth.

### Fixed
- App-drawer swipe is reliable. The bottom trigger sat inside Android's home-gesture
  band, which won the first swipe on gesture navigation; commit was distance-only, so
  a fast flick lifted before reaching it; and the swipe zones were the only way in.
  The drawer is now finger-tracked, opens on any upward drag, and a nested-scroll gate
  lets the task list and calendar agenda scroll first and open it with what they cannot
  consume.
- All-day calendar events no longer consume free time. A fortnight-long all-day entry
  blocked every hour inside it, pinning the free-hours figure to zero for its duration.
- Top insets are measured rather than guessed, so headers no longer crowd the status
  bar; content stays clear of the display cutout in fullscreen.
- The alignment task list no longer runs underneath its floating buttons.

### Changed
- The type scale is complete. Nine of Material's fifteen text styles were undefined
  and fell back to Roboto — roughly half the text in the app, including every settings
  title and description. Sixteen call sites also requested font weights that are not
  bundled, two of which made Compose synthesise fake bold.
- Tasks render as type rather than cards, matching the app drawer and the alignment
  screen, with a left rule that aligns with the calendar agenda beneath them.
- The accent does more work: deadlines carry it, the progress bar's track is tinted
  with it, the free-hours figure is coloured by how much of the horizon is unclaimed,
  and calendar colours are pulled toward it so the agenda reads as one palette.
- The app drawer's long-press actions are words rather than icons, grouped by what
  they act on, with Uninstall the only one marked.

## [3.3.0] — 2026-06-25

### Fixed
- App drawer no longer crashes the launcher while filtering: a package exposing more
  than one launcher activity produced duplicate list keys (`IllegalArgumentException`
  in `LazyColumn`), which only surfaced once a search narrowed the list. Drawer entries
  now use a key that includes the launch activity, so they stay unique.
- App-drawer "Uninstall" action now opens the system uninstall dialog. It silently
  did nothing before because the `REQUEST_DELETE_PACKAGES` permission was not declared.
- Calendar agenda no longer shows a deleted occurrence of a recurring series (cancelled
  exception rows are now filtered by `STATUS`).
- Tapping a calendar event opens the tapped occurrence instead of the series' first
  occurrence: the per-occurrence begin/end times are now forwarded to the calendar app.

### Changed
- The default accent colour now follows the system Material You palette (was a fixed
  green). Existing installs keep their saved colour; presets and a custom hex colour
  remain available in Settings.

## [3.2.0] — 2026-06-22

### Changed
- Relicensed from the MIT License to the Mozilla Public License 2.0 (MPL-2.0).
  MPL-2.0 is file-level copyleft: the source stays free and open, modifications to
  existing files must remain MPL-2.0, but it can be combined with other code in a
  larger work. Earlier releases remain under their original MIT terms.

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
