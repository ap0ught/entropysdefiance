# Submitting Entropy's Defiance to F-Droid

This file is a one-page reference for shipping a release to F-Droid /
IzzyOnDroid. It is not built into the app.

## Per-release checklist

1. Bump `versionCode` and `versionName` in `app/build.gradle.kts`.
2. Add a top entry to `CHANGELOG.md` and a matching
   `fastlane/metadata/android/en-US/changelogs/<versionCode>.txt`
   (one short paragraph per release, max ~500 chars; F-Droid truncates).
3. If you've added screenshots, drop them in
   `fastlane/metadata/android/en-US/images/phoneScreenshots/` as
   `1.png`, `2.png`, ….
4. Run locally:
   ```sh
   JAVA_HOME=/usr/lib/jvm/java-17-openjdk ./gradlew test lintDebug assembleRelease
   ```
   `assembleRelease` should succeed without `KEYSTORE_*` env vars (it produces
   an unsigned APK; F-Droid signs with their own key).
5. Tag the commit and push:
   ```sh
   git tag v<versionName>
   git push origin v<versionName>
   ```
   The GitLab CI release pipeline still uploads a signed APK to the GitLab
   Package Registry for users who install directly.

## First submission to F-Droid

F-Droid's recipe lives in the
[fdroiddata](https://gitlab.com/fdroid/fdroiddata) repository. Open a merge
request adding `metadata/com.hwanderer.entropysdefiance.yml`:

```yaml
Categories:
  - Time
License: MPL-2.0
SourceCode: https://gitlab.com/hwanderer/entropysdefiance
IssueTracker: https://gitlab.com/hwanderer/entropysdefiance/-/issues
Donate: https://buymeacoffee.com/hwanderer

AutoName: Entropy's Defiance

RepoType: git
Repo: https://gitlab.com/hwanderer/entropysdefiance.git

Builds:
  - versionName: 3.0.0
    versionCode: 3
    commit: v3.0.0
    subdir: app
    gradle:
      - yes

AutoUpdateMode: Version
UpdateCheckMode: Tags
CurrentVersion: 3.0.0
CurrentVersionCode: 3
```

Once merged, F-Droid's bot picks up subsequent tags automatically.

## IzzyOnDroid (faster, recommended for first appearance)

[IzzyOnDroid](https://apt.izzysoft.de/fdroid/) is a third-party F-Droid-format
repo that ships within hours rather than days. It auto-discovers the fastlane
metadata in this repo. To list the app, open an issue at
https://github.com/IzzySoft/IzzyOnDroid.github.io/issues asking for inclusion
and pointing to a tagged release on GitLab.

## Anti-features to disclose

When the fdroiddata MR lands, F-Droid reviewers may flag:

- **`QUERY_ALL_PACKAGES`** — required for the launcher to enumerate apps;
  document this in the MR description.
- **`BIND_DEVICE_ADMIN`** — only requested if the user opts in to Zen Mode's
  Do Not Disturb integration. Make this explicit so it isn't tagged
  `NonFreeAdd`.

There are no trackers, no proprietary network calls, and no advertising
libraries — confirm with `exodus-standalone` if asked.
