# Agent Guidelines

## Build/Test Commands
- **Android build**: `./gradlew assembleDebug` (use Java 17: `JAVA_HOME=/usr/lib/jvm/java-17-openjdk`)
- **Run tests**: `./gradlew test` | Single test: `./gradlew test --tests "*TestClassName.methodName"`
- **Lint**: `./gradlew lint` | **Type check**: `./gradlew compileDebugKotlin`
- **Python TUI**: `python3 tui_task_manager.py tasks.json` | Test: `python3 -m py_compile tui_task_manager.py`

## Kotlin (Android) Style
- **Architecture**: MVVM, Repository pattern, Room + KSP, DataStore, Coroutines/Flow, Compose + Material3
- **Naming**: Classes=PascalCase, functions=camelCase, constants=UPPER_SNAKE_CASE
- **Imports**: Group by Android→Compose→Kotlin→Project, alphabetical, avoid wildcards
- **Null safety**: Use `?` sparingly, prefer safe calls `?.` and Elvis `?:`, use `val` over `var`
- **Compose**: Use `@Composable`, `remember`, `LaunchedEffect`, `collectAsStateWithLifecycle()`
- **Testing**: JUnit + Mockito, descriptive names with backticks, Given-When-Then pattern
- **Packages**: `data/`, `model/`, `ui/` (components, screens, theme), `viewmodel/`, `utils/`

## Python (TUI) Style
- **Design**: Minimal, elegant, essential - avoid bloat, focus on content over decoration
- **Imports**: Rich library components, type hints with `Optional`, `List`, `Dict`
- **Functions**: Type-annotated, docstrings for public methods, single responsibility
- **UI**: Use Rich Panel/Group/Columns, simple box styles, color sparingly (red/yellow/blue/cyan/magenta)
- **File I/O**: JSON with proper error handling, atomic writes, clear success messages
- **Testing**: `python3 -m py_compile` for syntax, test with tempfiles

## Shared
- **JSON format**: Version 1, fields: id, title, timeHorizon, autoId, createdAt, isCompleted, isDeadlineTask, deadline, daysNeeded, doDate
- **Time horizons**: Today, ThisWeek, ThisMonth, ThisYear, ThisLife
- **Documentation**: Keep in README.md only, no separate docs unless absolutely essential
