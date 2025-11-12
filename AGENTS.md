# Agent Guidelines for Entropy's Defiance

## Build/Lint/Test Commands

### Building
- **Debug build**: `./gradlew assembleDebug`
- **Release build**: `./gradlew assembleRelease`
- **Install debug**: `./gradlew installDebug`

### Testing
- **Unit tests**: `./gradlew test`
- **Instrumentation tests**: `./gradlew connectedAndroidTest`
- **All tests**: `./gradlew test connectedAndroidTest`
- **Single test class**: `./gradlew test --tests "*TestClassName*"`
- **Single test method**: `./gradlew test --tests "*TestClassName.testMethodName"`

### Linting/Code Quality
- **Lint check**: `./gradlew lint`
- **Kotlin lint**: `./gradlew ktlintCheck` (if ktlint plugin is added)
- **Type check**: `./gradlew compileDebugKotlin` or `./gradlew compileReleaseKotlin`

## Code Style Guidelines

### Language & Framework
- **Language**: Kotlin with Jetpack Compose
- **Architecture**: MVVM with ViewModels, Repository pattern
- **Database**: Room with KSP code generation
- **Preferences**: DataStore Preferences
- **Async**: Coroutines with Flow
- **UI**: Material Design 3 with Jetpack Compose

### Imports
- Organize imports alphabetically
- Use wildcard imports sparingly
- Group: Android, Compose, Kotlin stdlib, project packages
- Example:
```kotlin
import android.content.Context
import androidx.compose.runtime.*
import androidx.lifecycle.viewModelScope
import com.example.entropysdefiance.model.Task
import kotlinx.coroutines.launch
```

### Naming Conventions
- **Classes**: PascalCase (e.g., `TaskViewModel`, `SettingsRepository`)
- **Functions/Methods**: camelCase (e.g., `addTask()`, `getPendingTasks()`)
- **Variables/Properties**: camelCase (e.g., `taskList`, `isCompleted`)
- **Constants**: UPPER_SNAKE_CASE (e.g., `LAST_UPDATE_DATE_KEY`)
- **Packages**: lowercase with dots (e.g., `com.example.entropysdefiance.data`)

### Code Structure
- Use data classes for models and simple DTOs
- Use sealed classes/enums for type-safe state representations
- Prefer immutable data structures
- Use `val` over `var` when possible
- Use meaningful, descriptive names

### Error Handling
- Use try-catch for expected exceptions
- Use Result<T> or custom sealed classes for complex error states
- Log errors appropriately with meaningful messages
- Show user-friendly error messages via Toast/Snackbar

### Null Safety
- Use nullable types (`?`) only when necessary
- Prefer non-null types with default values
- Use safe calls (`?.`) and Elvis operator (`?:`)
- Use `requireNotNull()` or `checkNotNull()` for required parameters

### Compose Guidelines
- Use `@Composable` functions for UI components
- Use `remember` for state that survives recomposition
- Use `LaunchedEffect` for side effects
- Use `collectAsStateWithLifecycle()` for Flow collection
- Use `@OptIn` for experimental APIs
- Prefer `Modifier` chaining for styling

### Testing
- Use JUnit 4/5 for unit tests
- Use Mockito for mocking dependencies
- Use Robolectric for Android framework mocking
- Use AndroidX Test for instrumentation tests
- Follow Given-When-Then pattern in test names
- Use descriptive test method names with backticks

### Dependencies
- Use version catalog (libs.versions.toml) for dependency management
- Prefer AndroidX libraries over legacy support libraries
- Use BOM (Bill of Materials) for Compose dependencies
- Keep dependencies updated and minimal

### File Organization
- Group related classes in packages (data, model, ui, viewmodel, utils)
- Use feature-based organization within ui package (components, screens, theme)
- Keep files focused on single responsibility
- Use consistent package structure across the app</content>
<parameter name="filePath">/home/jonas/areas/digitalism/entropysdefiance/AGENTS.md