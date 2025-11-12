# GitLab Production Pipeline for APK Releases

To set up a production pipeline on GitLab that builds APKs and makes them available for download in the Releases section, you'll need to configure CI/CD pipelines, build jobs, and release automation. Based on GitLab documentation and Android development practices, here's what it takes:

## 1. Configure CI/CD Pipeline (.gitlab-ci.yml)

- Create a `.gitlab-ci.yml` file in your repository root to define the pipeline.
- Use stages like `build`, `test`, and `release`.
- For Android projects, use a Docker image with Android SDK (e.g., `image: openjdk:11-jdk` or a pre-built Android image like `cimg/android:2024.01`).
- Key jobs:
  - **Build APK**: Run Gradle commands to assemble the APK (e.g., `./gradlew assembleRelease` for production builds). Store the output as artifacts.
  - **Test**: Run unit/instrumentation tests (e.g., `./gradlew test`, `./gradlew connectedAndroidTest`).
  - **Release**: Use GitLab's `release` keyword to create a release automatically.

Example basic `.gitlab-ci.yml` snippet:

```yaml
stages:
  - build
  - test
  - release

build_apk:
  stage: build
  script:
    - ./gradlew assembleRelease
  artifacts:
    paths:
      - app/build/outputs/apk/release/*.apk
    expire_in: 1 week

test:
  stage: test
  script:
    - ./gradlew test

release_job:
  stage: release
  script:
    - echo "Release APK"
  release:
    name: 'Release $CI_COMMIT_TAG'
    description: 'Release notes here'
    tag_name: '$CI_COMMIT_TAG'
    assets:
      links:
        - name: 'APK Download'
          url: '$CI_PROJECT_URL/-/jobs/$CI_JOB_ID/artifacts/file/app/build/outputs/apk/release/app-release.apk'
  only:
    - tags
```

## 2. Build and Store APKs

- Use Gradle tasks like `assembleRelease` to build signed APKs (configure signing in `build.gradle` with keystore).
- Store APKs as job artifacts (temporary, expire after a set time) or upload to GitLab's Generic Packages registry for permanent storage.
- For permanent downloads, publish to packages: Use `./gradlew publish` or GitLab CLI (`glab`) to upload to the package registry, then link in releases.

## 3. Automate Releases

- Releases are created via the `release` keyword in a CI job, triggered on tags (e.g., `only: - tags`).
- Attach assets: Link to APK artifacts or package registry URLs.
- Releases appear in **Deploy > Releases**, with downloadable assets.
- Permissions: Developers can create/edit releases; use protected tags for security.

## 4. Additional Requirements

- **GitLab Runner**: Ensure a runner with Android tools (or use GitLab.com shared runners).
- **Signing**: For production, sign APKs with a keystore (store securely as CI variables).
- **Testing**: Run lint (`./gradlew lint`) and tests before release.
- **Security**: Use protected branches/tags, masked variables for keys.
- **Triggers**: Pipelines run on pushes/merges; releases on tags.

For detailed examples, refer to GitLab docs on Android CI/CD and releases. If you need help implementing this in your project, provide more details on your setup.
