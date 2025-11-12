# Build Issues and Fixes

## Local Build Errors
- **Java Version Parsing Error**: The build fails with `IllegalArgumentException: 25.0.1` because Gradle/Kotlin cannot parse the Java version "25.0.1". This occurs during script compilation.

## Local Build Warnings
- **Restricted Method Access**: Warning about `java.lang.System::load` being called in an unnamed module. Suggests using `--enable-native-access=ALL-UNNAMED`.
- **Gradle Daemon Incompatibility**: 1 incompatible and 1 stopped daemon could not be reused.

## CI Build Warnings
- **Deprecated BuildConfig Setting**: `android.defaults.buildfeatures.buildconfig=true` is deprecated. Will be removed in AGP 10.0. Migrate to `android.buildFeatures.buildConfig = true` in module-level build.gradle.
- **Unable to Strip Libraries**: Unable to strip `libdatastore_shared_counter.so`. Run with --info for more details.
- **Kotlin Compiler Warnings**: Numerous warnings about unused parameters/variables, deprecated APIs, and naming issues.
- **Test Failures**: SettingsRepositoryTest failing due to Robolectric package parsing issues and improper mocking.

## Task List to Fix Build

### Local Build Fixes
- [ ] Switch to a supported Java version (e.g., Java 21 or 17) for running Gradle, as Java 25 is not recognized by Gradle 8.13.
- [ ] Add `--enable-native-access=ALL-UNNAMED` to `org.gradle.jvmargs` in `gradle.properties` to suppress the restricted method warning.
- [ ] Verify that the project compiles with the new Java version by running `./gradlew assembleDebug` again.
- [ ] If issues persist, consider updating Gradle wrapper to a version that supports Java 25 (if available).

### CI Build Fixes
- [x] Migrate deprecated `android.defaults.buildfeatures.buildconfig=true` to `android.buildFeatures.buildConfig = true` in `app/build.gradle.kts`.
- [x] Investigate and fix unable to strip `libdatastore_shared_counter.so` library. (Add `packaging { doNotStrip "**/libdatastore_shared_counter.so" }` to `android` block in `app/build.gradle.kts`.)
- [x] Replace deprecated `Icons.Filled.ArrowBack` with `Icons.AutoMirrored.Filled.ArrowBack` in:
  - `AddTaskScreen.kt`
  - `AppearanceSettingsScreen.kt`
  - `BackupSettingsScreen.kt`
  - `CalendarSelectionDialog.kt`
  - `CalendarSettingsScreen.kt`
  - `GesturesSettingsScreen.kt`
  - `StatisticsScreen.kt`
  - `TaskList.kt`
- [x] Replace deprecated `Icons.Filled.ArrowForward` with `Icons.AutoMirrored.Filled.ArrowForward` in:
  - `AlignmentScreen.kt` (2 instances)
  - `SettingsDialog.kt` (2 instances)
  - `TaskList.kt`
- [x] Replace deprecated `Divider` with `HorizontalDivider` in:
  - `AppDrawer.kt`
  - `CalendarAgendaWidget.kt`
  - `CalendarSelectionDialog.kt`
- [x] Replace deprecated `consumeAllChanges()` with `consume()` in `AppDrawer.kt` (2 instances).
- [x] Replace deprecated `outlinedTextFieldColors(...)` with `OutlinedTextFieldDefaults.colors` in `AddTaskScreen.kt`.
- [x] Remove or use unused parameters/variables in `MainActivity.kt`, `AppDrawer.kt`, `AppearanceSettingsScreen.kt`, `BackupSettingsScreen.kt`, `CalendarAgendaWidget.kt`, `CalendarSelectionDialog.kt`, `CalendarSettingsScreen.kt`, `ColorPaletteSelectionDialog.kt`, `GesturesSettingsScreen.kt`, `SettingsDialog.kt`, `StatisticsScreen.kt`, `ZenModeManager.kt`, `ZenModeViewModel.kt`.
- [x] Rename parameters in `TaskDatabase.kt` Migration classes to match supertype naming (`db`).
- [x] Remove unnecessary non-null assertion (`!!`) on `LocalDate` in `TaskViewModel.kt`.
- [x] Fix name shadowing of `screenHeight` in `MainActivity.kt`.
- [x] Fix SettingsRepositoryTest failures by refactoring repository for testability and suppressing unchecked cast warning.