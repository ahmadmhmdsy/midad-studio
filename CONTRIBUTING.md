# Contributing to Midad Studio

Thank you for taking the time to look at Midad Studio. This repository is **source-available but not open
source**: reading, studying, building for personal evaluation and sending issues or pull requests are welcome, but
redistribution of the code or of any build is not allowed. Read [LICENSE](LICENSE) before you contribute;
contributions are accepted under those terms.

Midad Studio is an offline-first Android creative studio (photo editor, collage/design studio, video editor and
on-device AI background removal) with Arabic and English as first-class languages. `0.2.3` (versionCode 5) is the
current **signed pre-release**. It is **not published on Google Play** and has not been submitted to Google Play;
the only public builds are the signed APK/AAB attached to GitHub Releases. Please keep that status in mind when
writing bug reports and documentation.

## Before you start

1. Read the [README](README.md), [the build report](docs/qa/BUILD_REPORT.md) and the feature notes you are touching
   (for example [PREVIEW_AND_RESULTS.md](docs/product/PREVIEW_AND_RESULTS.md) for the video preview and export
   result dialog, or [PROJECTS.md](docs/product/PROJECTS.md) for project storage).
2. Understand the project's invariants (below). A change that breaks one of them is not a small change.
3. For anything that adds a permission, a network request, a third-party SDK or a new AI model, open an issue and
   get a maintainer decision **before** writing the code.
4. Do not mention or compare the project with other commercial apps - in code, docs, issues or pull requests.

### Project invariants

- **Offline-first.** Photos and videos are opened through the Android Photo Picker, processed in memory and
  written back through MediaStore. Media never leaves the device.
- **No ads, accounts, analytics, telemetry or crash-reporting SDKs.** None exists in `0.2.3`, and none will be
  merged without a broader product, privacy-policy and store-listing change agreed in an issue first.
- **One permission: `INTERNET`.** It exists only for a user-initiated AI model download. Do not add another
  `<uses-permission>` and do not add any other network call.
- **Backup and device transfer stay disabled.** App data (including projects and drafts) lives in app-private
  storage excluded from backup.
- **Arabic is a first-class locale.** Every user-visible string exists in English and Arabic, and the Arabic
  layout must work in RTL.
- **Claims must be evidence-based.** Documentation, README text and PR descriptions may only state what the code,
  a test run or a signed artifact proves. Mark unverified things as unverified; never write "device-tested",
  "store-ready" or a performance number without the corresponding evidence.

## Ways to contribute

- **Bug reports** - use the bug report issue form. Include the app version, install source, device/Android version
  and a minimal reproduction. Remove personal photos and private data from screenshots and logs.
- **Feature requests** - use the feature request form. Explain the user problem first. Features that need new
  permissions, a server, cloud sync, ads or generative AI are outside the current product scope.
- **Pull requests** - small, focused changes with tests and documentation updates. See the checklist below.
- **Translations and copy** - Arabic and English copy live together in the string files; improvements are welcome
  when they keep both locales in parity.
- **Security reports** - never in a public issue. Follow [SECURITY.md](SECURITY.md).
- **Community behaviour** - all participation is covered by [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md).

## Development requirements

| Requirement | Version used by the project |
|---|---|
| JDK | **17 or newer** to run Gradle (the app targets Java 17 bytecode; CI uses Temurin 21) |
| Android SDK | **platform 36** and **build-tools 36.0.0** |
| Gradle | **8.11.1** through the checked-in wrapper - do not substitute a different Gradle or AGP version |
| Android Studio | Any recent stable release; not required (command-line builds are the reference) |
| Device / emulator | **Android 8.0 (API 26)** minimum; API 36 emulators for the instrumented suite |

Create `local.properties` with your SDK path (it is gitignored), or let Android Studio create it:

```properties
sdk.dir=C\:\\Users\\you\\AppData\\Local\\Android\\Sdk
```

The wrapper pins Gradle 8.11.1 and the root build pins AGP 8.10.1 / Kotlin 2.0.21. A different Gradle or AGP
version changes the merged manifest and the native-library packaging, both of which the release checks depend on.

## Build and test

Windows (PowerShell / cmd):

```powershell
.\gradlew.bat :app:assembleDebug --console=plain
.\gradlew.bat :app:testDebugUnitTest --console=plain
.\gradlew.bat :app:lintDebug --console=plain
# Instrumented checks need an emulator or a device:
.\gradlew.bat :app:connectedDebugAndroidTest --console=plain
```

macOS / Linux:

```bash
./gradlew :app:assembleDebug --console=plain
./gradlew :app:testDebugUnitTest --console=plain
./gradlew :app:lintDebug --console=plain
./gradlew :app:connectedDebugAndroidTest --console=plain
```

Outputs:

- debug APK: `app/build/outputs/apk/debug/app-debug.apk`
- unit-test report: `app/build/reports/tests/testDebugUnitTest/index.html`
- lint report: `app/build/reports/lint-results-debug.html`
- Robolectric screenshot tests write PNGs to `app/build/ui-render/` by default, or to the directory named by the
  `midad.renderDir` system property (both ignored by git)

**Release builds are different.** `:app:assembleRelease` and `:app:bundleRelease` require a signing
configuration (`keystore.properties` plus the keystore file). They deliberately fail configuration when the
credentials are missing, so a contributor without signing material cannot produce a release artifact by accident.
Release artifacts are built and signed by the maintainer, never by CI.

### What CI runs

The `Android` workflow runs on every push to `main`, on pull requests and on manual dispatch: it sets up JDK 21,
runs `:app:testDebugUnitTest` and reports `lintDebug` (lint is currently reported, not blocking, while the
warning baseline is cleaned up). CI has read-only repository access and no signing secrets.

## Project layout

| Path | Contents |
|---|---|
| `app/src/main/java/com/midad/studio/core/` | shared infrastructure: export, drafts, projects, AI model storage and inference bridge, preferences |
| `app/src/main/java/com/midad/studio/studio/` | the studios (photo, design/collage, video, AI) and their engines/view-models |
| `app/src/main/java/com/midad/studio/ui/` | app-wide Compose theme, shared screens and widgets (including the export result dialog) |
| `app/src/main/java/com/midad/studio/navigation/` | routes and the navigation host |
| `app/src/main/res/values/strings_*.xml` | English strings, split per area |
| `app/src/main/res/values-ar/strings_*.xml` | Arabic strings, same file and key names |
| `app/src/main/assets/models/` | bundled models plus `NOTICE.txt`, the Apache-2.0 text and the LiteRT licence/notice files |
| `app/src/test/` | JVM and Robolectric tests |
| `app/src/androidTest/` | instrumented checks that use real device storage and MediaStore |
| `docs/` | public product, privacy, model-licence and QA documentation |
| `tools/` | release and 16 KB page-size check scripts |

## Code conventions

### Kotlin and Compose

- Kotlin 2.0.21, Jetpack Compose with Material 3, Java 17 bytecode. Match the existing style; the project uses
  `kotlin.code.style=official`.
- Keep the package boundaries: shared behaviour belongs in `core/`, studio-specific UI and logic in `studio/<area>/`.
  Do not import one studio from another; a shared need goes through `core/` or `ui/`.
- Keep state holder/ViewModel work off the main thread; use coroutines for media and file work. Do not swallow
  `CancellationException` - rethrow it after cleanup.
- Prefer the existing helpers (export manager, project repository, draft stores, number formatter) over new
  parallel implementations of the same thing.

### Strings and localisation

- Add every user-visible string to the matching file in **both** `res/values/` and `res/values-ar/`, with the same
  key. Keys marked `translatable="false"` may exist only in the default locale.
- Format arguments must match between locales: the same `%1$s`-style placeholders in both strings.
- `LocalizationParityTest` fails the build if the two locale file sets differ, a key is missing or extra, an
  Arabic value is blank, a format argument differs or a key is duplicated. Run it with the unit tests.
- Use `MidadNumbers` for numbers that should follow the locale (sliders, percentages, brush sizes). Technical
  values such as file names, formats and version numbers stay in Western digits.
- Check the RTL layout of any screen you touch; a layout that only looks right in English is a bug.

### Privacy and network

- Do not add analytics, crash reporting, advertising, attribution or tracking libraries.
- Do not add a network call. The only permitted network use is the user-initiated model download through the
  existing repository, with an HTTPS URL pinned in the model manifest and a SHA-256 check before the model is used.
- Do not request broad storage permissions. Media comes in through the Photo Picker and goes out through
  MediaStore; legacy sharing uses the existing non-exported `FileProvider`.
- Do not re-enable Android backup or device transfer or move app data outside the app-private directories.

### AI models and licences

- A new model must have a licence that permits commercial use (Apache-2.0/MIT/BSD-style); non-commercial or
  restricted models must not ship.
- Record the licence name, evidence URL, fetch date, exact size and SHA-256 in
  [docs/product/MODEL_LICENSES.md](docs/product/MODEL_LICENSES.md) and the model manifest, update
  [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) and `app/src/main/assets/models/NOTICE.txt`, and add the licence
  text under `docs/product/licenses/`.
- A downloaded model must be pinned to a URL and verified by SHA-256 before use, stored in app-private storage, and
  must never be installed or replaced without a user action.

### Repository hygiene - never commit

These are ignored by `.gitignore`; keep them out of commits as well:

- `keystore.properties` and any `.jks` / `.keystore` file (signing material)
- `local.properties` (machine-specific SDK path)
- `build/`, `app/build/`, `.gradle/`, `.kotlin/`
- `*.apk`, `*.aab`, `*.apks`, R8 `mapping.txt` and other build outputs
- `tmp/` and `.dsh/` (local scratch and tool state)

Also: no signing secrets in CI, no committed artifacts, no generated files that the build can recreate.

### Documentation and claims

- Every factual claim in documentation, README text, issue replies or pull requests must be traceable to the code,
  a test run, the build report or a signed artifact. If it is not verified, say so explicitly.
- Do not claim a device run, a benchmark, a store rating, a user count, a trademark clearance, Google Play
  availability or any comparison with another app.
- User-visible changes need a line under `[Unreleased]` in [CHANGELOG.md](CHANGELOG.md); build-evidence changes
  need the corresponding update in [docs/qa/BUILD_REPORT.md](docs/qa/BUILD_REPORT.md) with the command and result.
- Keep documentation links pointing at files that are actually published in this repository. Do not link private
  working notes.
- Update the Arabic copy of the README and public pages when the English copy changes materially (or note the gap
  in the PR if a translator pass is needed).

## Pull request checklist

Use the pull-request template and confirm every item that applies:

- [ ] The change is focused and explained (problem, approach, alternatives considered).
- [ ] `./gradlew :app:assembleDebug` succeeds.
- [ ] `./gradlew :app:testDebugUnitTest` passes, and a test covers the behaviour change where practical.
- [ ] `./gradlew :app:lintDebug` introduces no new error; new warnings are explained.
- [ ] For instrumented or storage changes: `./gradlew :app:connectedDebugAndroidTest` passes on an API 36
      emulator/device and the result is included in the PR.
- [ ] Every added or changed user-visible string exists in both `values/` and `values-ar/` and the parity test
      passes.
- [ ] RTL layout checked for UI changes (screenshot or description included).
- [ ] No new permission, network call, telemetry/analytics/ad SDK or cloud dependency.
- [ ] No new third-party runtime dependency without a licence check recorded in
      [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) and [docs/product/MODEL_LICENSES.md](docs/product/MODEL_LICENSES.md)
      for AI models.
- [ ] No signing material, `local.properties`, build output or generated artifact is committed.
- [ ] Documentation, `CHANGELOG.md` and, if build evidence changed, the build report are updated.
- [ ] No competitor names, no unverified claims and no Google Play availability claim.
- [ ] Screenshots (if UI changed) contain no personal photos or private data.

## Issues

- Search existing issues before opening a new one.
- Use the issue forms. For a bug, include the app version and versionCode (`0.2.3`, versionCode 5), how you
  installed the app, your device and Android version, the steps, the expected result and what actually happened.
- Attach a screenshot or a log only if it contains no personal media. Redact file names and paths if they identify
  you.
- Feature requests: check the README's "what is in the box" and the known limits first. Out of scope for `0.2.x`:
  cloud sync or accounts, ads, generative imagery, music/audio import, speed control, portable project files, new
  permissions.
- This is a single-maintainer project; replies are best-effort and there is no support SLA.
- Anything that looks like a security or privacy problem: do **not** open a public issue, use
  [SECURITY.md](SECURITY.md).

## Review and merge

- Pull requests target `main`. Keep one logical change per pull request; large refactors should start as an issue.
- A maintainer reviews for scope, the invariants above, Arabic/English parity, licence compatibility and claim
  honesty. Review comments may ask for a smaller change or for evidence before a claim is accepted.
- By submitting a pull request you confirm that you wrote the change or otherwise have the right to submit it, and
  that it may be distributed as part of this repository under its [LICENSE](LICENSE). The licence does not transfer
  the project's copyright.
- Merged changes are credited in the changelog when they are user-visible.

## Maintainer release notes

Release artifacts are built, signed and uploaded by the maintainer from a clean tree:

1. Set the version in `app/build.gradle.kts` and add the changelog entry.
2. Run the unit tests, `lintRelease`, `:app:assembleRelease` and `:app:bundleRelease` with the local keystore.
3. Run `pwsh -NoProfile -File tools/release-check.ps1` and record the version, versionCode, sizes and SHA-256
   values in [docs/qa/BUILD_REPORT.md](docs/qa/BUILD_REPORT.md).
4. Archive the R8 `mapping.txt` next to the AAB hash (it is not committed), tag the release and attach the signed
   APK/AAB to the GitHub Release.

Do not upload a debug-signed APK, do not publish an unsigned bundle and do not rebuild an artifact after recording
its hashes.
