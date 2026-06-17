# Releasing & F-Droid Publication

How a new version of **Entropy's Defiance** gets published. The app lives in the
main F-Droid repository: F-Droid builds every version **itself, from this GitLab
source**, on its own infrastructure and signs it with its own key. We never push
APKs to F-Droid. We just tag releases here; F-Droid's bot detects new tags and
proposes the build.

Read this whole file before cutting a release. Every box must be checked or the
F-Droid auto-build can silently break or stall.

## How the pipeline fits together

1. We bump the version and push a git tag (e.g. `v3.1.0`).
2. GitLab CI builds/signs an APK for *our* GitLab Releases page (this is **our**
   distribution channel — separate from F-Droid, see `.gitlab-ci.yml`).
3. F-Droid's bot runs `fdroid checkupdates` (~daily), sees the new tag +
   incremented `versionCode`, and opens a merge request against `fdroiddata`.
4. A maintainer merges it; F-Droid's build server compiles from source and signs.
5. The new version reaches users a few days later.

The recipe that controls step 3 lives in the **`fdroiddata`** repo, not here:
`https://gitlab.com/fdroid/fdroiddata/-/blob/master/metadata/com.hwanderer.entropysdefiance.yml`
For full automation it must have `UpdateCheckMode: Tags` and `AutoUpdateMode: Version v%v`.
If `AutoUpdateMode` is `None`, every update needs a manual MR to `fdroiddata`.

## Release checklist

### 1. Version bump
- [ ] In `app/build.gradle.kts`, increment **`versionCode`** by exactly 1
      (monotonic, no gaps) and set **`versionName`** to the new semver string.
- [ ] `versionCode` and the new git tag must move together. The current values
      are the source of truth — read them, don't assume.

### 2. Changelog
- [ ] Add `fastlane/metadata/android/en-US/changelogs/<versionCode>.txt`
      (filename = the **new** `versionCode`, e.g. `5.txt`, not the versionName).
- [ ] Keep it user-facing and concise; this text is shown verbatim in F-Droid.
- [ ] If anything else under `fastlane/metadata/.../en-US/` changed (title,
      descriptions, screenshots), confirm it's accurate — F-Droid pulls it from here.

### 3. Verify the build (locally)
- [ ] `JAVA_HOME=/usr/lib/jvm/java-17-openjdk ./gradlew assembleRelease`
- [ ] `./gradlew test`
- [ ] `./gradlew lint`

### 4. F-Droid build-safety review (the part that's easy to miss)
F-Droid rebuilds from source in a clean, network-restricted environment with
**no proprietary dependencies allowed**. Before tagging, confirm:
- [ ] No new closed-source/non-free dependency, and nothing from Google Play
      Services / Firebase / GMS was added.
- [ ] No new dependency that pulls binaries at build time or needs network access
      beyond declared Maven repos.
- [ ] Build tooling changes (Gradle, AGP, Kotlin, SDK, JDK) are intentional — these
      most often break F-Droid's recipe and may need a manual `fdroiddata` edit.
- [ ] App still builds offline-ish: no reliance on prebuilt artifacts checked into
      the repo, no reproducible-build-breaking changes.
- [ ] If any of the above changed, plan to also open an MR (or expect a
      maintainer ping) updating the `fdroiddata` metadata.

### 5. Commit, tag, push
- [ ] Commit the version bump + changelog together.
- [ ] Tag with the `v<versionName>` convention, e.g. `git tag v3.1.0`.
      Use the **same scheme** as existing release tags so `UpdateCheckMode: Tags`
      matches. Do **not** create stray tags that don't correspond to a
      `versionCode` bump — they confuse `fdroid checkupdates`.
- [ ] `git push && git push --tags`.

### 6. After pushing
- [ ] Confirm GitLab CI went green (build → test → release stages).
- [ ] Within a day or two, check that an auto-MR appeared in `fdroiddata`
      (or that the version shows up at
      `https://f-droid.org/packages/com.hwanderer.entropysdefiance/`).
      If nothing happens after several days, the recipe's `AutoUpdateMode` is
      likely `None` or the tag/versionCode didn't line up — investigate before
      retagging.

## Notes & gotchas
- F-Droid updates are **not instant** — expect a few days of lag vs. an instant
  store push.
- Never reuse or decrement a `versionCode`; F-Droid will reject it.
- The GitLab CI release and the F-Droid release are **independent**. A green CI
  pipeline does not mean F-Droid has shipped — they're separate channels.
