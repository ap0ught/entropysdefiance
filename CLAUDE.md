# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**Entropy's Defiance** is a minimalist Android launcher and task management app. Tasks are organized into five time horizons (Today, ThisWeek, ThisMonth, ThisYear, ThisLife) and synced to a JSON file for cross-device use via Syncthing. There is also a Python terminal UI for laptop access.

## Releasing

This app is published in the main F-Droid repo, which **rebuilds every version from
this GitLab source** and proposes updates via a bot watching git tags. Cutting a
release has strict requirements (versionCode/changelog/tag alignment, no proprietary
deps, build-tooling caution). **Before working on any release or version bump, read
[`RELEASING.md`](RELEASING.md) and follow its checklist.**

## Build & Test Commands

```bash
# Build (use Java 17)
JAVA_HOME=/usr/lib/jvm/java-17-openjdk ./gradlew assembleDebug
./gradlew assembleRelease

# Testing
./gradlew test
./gradlew test --tests "*TestClassName.methodName"   # single test

# Lint & type check
./gradlew lint
./gradlew compileDebugKotlin

# Python TUI
pip install -e .
entropysdefiance                                    # or: ed-tui
python3 -m py_compile tui_task_manager.py          # syntax check
```

## Architecture

**Pattern**: MVVM + Repository, all UI in Jetpack Compose + Material 3.

**Layers**:
- `model/` — Data classes: `Task`, `AppSettings`, `TimeHorizon`, `CalendarEvent`
- `data/` — Repositories abstracting storage: `TaskFileRepository` (JSON), `SettingsRepository` (DataStore), `CalendarRepository`, `BackupRepository`
- `viewmodel/` — StateFlow-based state management: `TaskViewModel`, `SettingsViewModel`, `CalendarViewModel`, `LauncherViewModel`, etc.
- `ui/components/` & `ui/screens/` — Compose UI; settings are split into per-topic screens (appearance, calendar, gestures, file sync, backup, permissions)
- `utils/` — Calendar calculations, Zen mode helpers, time horizon logic

**Key data flows**:
- Tasks persist as JSON files; `TaskFileRepository` polls the file every 2 seconds to detect external edits (Syncthing integration).
- Settings persist via DataStore (`SettingsRepository`); deadline tracking uses SharedPreferences.
- `EntropysDefianceApplication` initializes `SettingsRepository` at startup; `MainActivity` hosts the single-activity Compose app.
- File access uses SAF URIs (Android 13+) or plain file paths (older).

**Task JSON format** (version 1):
```json
{
  "version": 1,
  "tasks": [
    {
      "id": "...", "title": "...", "timeHorizon": "Today",
      "autoId": 1, "createdAt": "...", "isCompleted": false,
      "isDeadlineTask": false, "deadline": null,
      "daysNeeded": null, "doDate": null
    }
  ]
}
```
`timeHorizon` values: `Today`, `ThisWeek`, `ThisMonth`, `ThisYear`, `ThisLife`.

## Coding Conventions

- **Naming**: Classes=PascalCase, functions=camelCase, constants=UPPER_SNAKE_CASE
- **Imports**: Group Android → Compose → Kotlin → Project; alphabetical; no wildcards
- **Null safety**: Prefer `?.` and `?:`, favor `val` over `var`
- **Compose**: Use `@Composable`, `remember`, `LaunchedEffect`, `collectAsStateWithLifecycle()`
- **Testing**: JUnit + Mockito, backtick test names, Given-When-Then pattern
- **Python TUI**: Rich library, type hints, `Optional`/`List`/`Dict`; atomic JSON writes; minimal decoration

## Platform Notes

- Min SDK 33 (Android 13+), compile SDK 35
- Kotlin 1.9.22, Kotlin Serialization for JSON, Gradle 8.13
- Single activity (`MainActivity`), portrait orientation
- `ZenModeDeviceAdmin` receiver handles Do Not Disturb management
