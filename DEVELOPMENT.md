# Development Notes

## Local Java Version Configuration

If you're running Java 25+ locally and encounter build issues with Kotlin 1.9.22, you can configure Gradle to use Java 17:

### Option 1: Edit gradle.properties (Recommended for persistent local setup)

1. Uncomment and adjust the path in `gradle.properties`:
   ```properties
   org.gradle.java.home=/usr/lib/jvm/java-17-openjdk
   ```

2. Tell Git to ignore your local changes:
   ```bash
   git update-index --assume-unchanged gradle.properties
   ```

3. If you need to commit changes to gradle.properties later:
   ```bash
   git update-index --no-assume-unchanged gradle.properties
   ```

### Option 2: Use JAVA_HOME environment variable (For one-off builds)

```bash
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk
./gradlew clean build
```

### Option 3: Use Gradle daemon JVM args

Add to `~/.gradle/gradle.properties` (global, won't affect repository):
```properties
org.gradle.java.home=/usr/lib/jvm/java-17-openjdk
```

## Running Tests

```bash
# Run all tests
./gradlew test

# Run specific test class
./gradlew test --tests "SettingsRepositoryTest"

# Run with clean build
./gradlew clean test
```

## CI/CD Note

GitLab CI uses the `cimg/android:2024.01` Docker image which has its own Java version. The `gradle.properties` file should NOT contain uncommented `org.gradle.java.home` settings that might not exist in the CI environment.
