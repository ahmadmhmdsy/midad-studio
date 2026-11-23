# Changelog

All notable changes to **Midad Studio** are documented in this file.

The format is based on [Keep a Changelog 1.1.0](https://keepachangelog.com/en/1.1.0/), and this project uses
[Semantic Versioning 2.0.0](https://semver.org/spec/v2.0.0.html) for version names. The Android `versionCode` is a
separate, monotonically increasing integer.

> **Release status.** `0.2.3` (versionCode 5) is the current **signed pre-release**, built 2026-11-22 18:34 UTC.
> It is **not published on Google Play**, and nothing has been submitted to Google Play; the signed APK and AAB are
> GitHub Release assets only. Build, test and lint evidence is in
> [docs/qa/BUILD_REPORT.md](docs/qa/BUILD_REPORT.md).
>
> Only `v0.2.3` has a release tag. The earlier baselines (`0.1.0`-`0.2.2`) were developed and verified privately
> and were never published, so they have no release links. Each entry lists the known limits that applied at the
> time; a limit carries forward unless a later entry records a fix.

## Unreleased

Nothing yet.

## [0.2.3] - 2026-11-22

Signed pre-release, versionCode 5.

### Added

- **Full-project video preview.** "Preview" in the export bar and "Preview full project" in the clip preview panel
  play the same Media3 composition that the exporter writes, so clip order and trims, colour look, output frame,
  mute state and title are shown together. Playback is a 960 px long-edge proxy; the exported file keeps the
  project's output size. The dialog pauses when the app is backgrounded, releases its player, and is disabled while
  an export is running.
- **One export result dialog for every studio.** After a photo, design/collage, AI cut-out or video export, the
  same card shows a thumbnail (checkerboard behind transparency; video frame with a play mark), the file name, the
  save location (`Pictures/Midad`, `Movies/Midad` or app storage) and **Open / Share / Close**. Open uses
  `ACTION_VIEW` with a read grant and falls back to a Toast when no viewer handles the file; Share keeps the
  existing non-exported `FileProvider` path.
- **AI model catalogue 4 -> 6.** DeepLab-V3 (general objects, pets, products and scenery; 21 classes;
  2,780,176 B) and MODNet (portraits only, soft hair edges; 26,166,776 B) are added as **download-only** entries.
  Both are Apache-2.0, fetched only when the user asks, and verified by SHA-256 before use.
- **Try on every installed model**, bundled or downloaded: it opens background removal with that model selected.
  Remove remains available for every model, and the picker shows each model's name and description.
- **NCHW tensor support.** The LiteRT bridge accepts NHWC and NCHW 4-D float32 inputs; each model's layout,
  activation and preprocessing are declared in the manifest instead of being inferred.
- **Licence surfaces.** The build now bundles the LiteRT licence and its third-party notice, and the refreshed
  `NOTICE.txt` covers all six models; the in-app licence viewer opens each entry (LiteRT licence, LiteRT
  third-party attributions, model credits, the Apache-2.0 text and every model row).

### Changed

- The final signed pair was rebuilt at **18:34 UTC** after the licence surfaces were refreshed (the earlier build
  still named four models and the old TensorFlow Lite coordinate); `tools/release-check.ps1` passed on the
  rebuilt pair.
- Automatic model choice now prefers Selfie Multiclass, then MODNet, then the two bundled selfie models, then
  DeepLab-V3; an explicit model choice always wins.
- Model downloads still come from two user-initiated model-file hosts (`storage.googleapis.com` for the MediaPipe
  files, `huggingface.co` for the MODNet conversion). No background request and no other endpoint were added.

### Fixed

- Stale model names and an obsolete TensorFlow Lite reference in the bundled `NOTICE.txt` and the in-app licence
  screen were corrected in the final 18:34 UTC build.

### Verification

- **94 unit/Robolectric tests in 20 suites**, 0 failures/errors/skips; **instrumented 10/10** (5/5 on each of two
  API 36 emulators).
- `lintRelease`: **0 errors / 62 warnings / 3 hints**, all 62 warnings triaged by category and none
  release-blocking.
- Release gate **PASSED** on the signed pair: versionCode 5 / versionName 0.2.3, `INTERNET` only,
  targetSdk 36 / minSdk 26, en + ar locales, not debuggable, nine native libraries 16 KB ELF-aligned, APK and AAB
  signatures verified.
- Emulator acceptance (API 36) on the 20:36 signed pair: photo export result dialog, six model cards, MODNet
  download -> install -> Try -> soft hair-edge cut-out, full-project preview (two clips, play/pause/seek) and the
  video export result dialog. The final 18:34 UTC rebuild was smoke-checked for the licence viewer only; the other
  flows were not re-run on it.

### Known limits

- **No physical-device run.** All acceptance is emulator evidence (API 36); a real-device pass is still
  outstanding.
- **The full preview is a scaled proxy**, not a pixel-identical re-export. It also relies on Media3 1.5.1's
  restricted `CompositionPlayer`, so it must be re-tested before any Media3 upgrade.
- **DeepLab-V3 is a general segmenter, not a portrait matte; MODNet handles portraits only.** No speed or memory
  benchmark was run for either model.
- Exports are flattened images: camera EXIF and the original colour profile are not carried over. The "Largest"
  photo export is capped at 3200 px.
- Video projects are capped at 3 minutes and 5 clips. There is no music library, audio import or speed control.
- Projects are local and app-private: no cloud sync, no project-file export or sharing, and no transfer to another
  device.
- The app has no ads, accounts, analytics or telemetry; the only network use is a user-initiated model download.
  The single `INTERNET` permission exists for that purpose.

## 0.2.2 - 2026-11-22

Signed pre-release, versionCode 4 (closed baseline).

### Added

- **Projects.** Save, reopen, rename and delete an editable session in Photo, Design/collage, AI cut-out and Video,
  with a thumbnail grid. Project payloads and thumbnails live in the app-private
  `noBackupFilesDir/projects` directory behind an atomically replaced, versioned index; no project file can be
  exported, shared or synced. See [docs/product/PROJECTS.md](docs/product/PROJECTS.md).
- **Video projects.** A project stores clip content URIs (with persistable read grants), trim points, look, frame
  ratio, mute and title as versioned JSON - it does not copy video bytes. A project is capped at 3 minutes and five
  clips, enforced on load and when adding clips with a visible notice rather than silent truncation.
- **Instrumented tests.** A four-class `androidTest` suite covering manifest permissions, export, photo drafts and
  model storage; 5/5 tests green on each of two API 36 emulators (10/10 total).
- Project reachability in the app: a Home card opens Projects/Exports, saved project titles appear in the studio
  toolbars, and long-press rename/delete on project cards uses localized labels.

### Changed

- Arabic plural and number polish: the models-ready string now has real Arabic plural forms, and slider,
  percentage, brush-size and export-quality values follow the locale through the app's number formatter.
- The photo export surface now discloses that exports are flattened images (camera EXIF and the original colour
  profile are not preserved).
- The video trim range is formatted as minutes:seconds.
- Leaving a studio with a bound project flushes the newest edit to the project first, then clears the draft; if the
  flush fails, the draft is kept so the edit is not lost.

### Fixed

- A draft-restore race that left a reopened AI cut-out project with the generic toolbar title; the saved project
  title and id are now bound deterministically.
- Hardened project loading: the index refresh drops rows whose directory has disappeared, and a project whose
  payload is missing shows a localized "could not be opened" message instead of an empty editor.
- Opening a project while an unfinished draft exists asks before replacing the draft, with Cancel and an explicit
  "Open anyway".

### Verification

- **90 unit/Robolectric tests in 18 suites**, 0 failures/errors/skips; instrumented 10/10 as above.
- `lintRelease`: **0 errors / 65 warnings / 3 hints**, triaged by category; none release-blocking.
- Release gate **PASSED** on the signed 0.2.2 pair; emulator smoke covered project create -> save -> back -> reopen,
  rename and delete (Photo, Design, AI cut-out and Video paths as recorded in the build report).

### Known limits

- No physical-device run; emulator evidence only.
- Projects do not sync, do not transfer between devices and cannot be imported or exported as files. A reinstall
  or a new phone does not bring projects back.
- A video project depends on the source clips still being readable; moved or deleted clips are skipped with a
  localized notice.
- Project thumbnails flatten transparency onto white, and project storage is not broken down per project in
  Settings.
- No music library, audio import or speed control in the video editor.

## 0.2.1 - 2026-11-19

Signed pre-release, versionCode 3 (closed baseline).

### Added

- **Typed export outcomes** (`NO_SPACE`, `IO`, `ENCODE`, `UNKNOWN`) with a free-space pre-check, localized
  failure messages and cleanup of failed exports.
- **Draft recovery after process death** for the Photo, Design/collage and AI cut-out studios: versioned binary
  drafts with app-owned pixel copies are restored on entry and deleted on explicit exit.
- 26 new unit tests (62 in total) covering the Photo, Design and AI draft codecs and stores and export-size
  planning.

### Changed

- The "Original" export option is renamed **"Largest"**, and its 3200 px long-edge cap is disclosed in the export
  sheet instead of being applied silently.
- Release builds **fail configuration** when `keystore.properties` is missing, so an unsigned AAB can no longer be
  produced by accident.
- Model download gains a free-space pre-check and a disk-full (storage) error classification; resume is still not
  implemented.
- The video export bar now respects the navigation-bar inset.

### Fixed

- Cancelled or failed exports could leave `IS_PENDING=1` MediaStore rows; the failure path now deletes its row and
  a startup sweep clears leftovers.
- Replacing a design image gives the new layer a fresh id, so undo can no longer restore a recycled bitmap (the
  photo editor's working-bitmap path is unchanged).

### Verification

- **62 unit/Robolectric tests**, 0 failures/errors/skips; `lintRelease` **0 errors / 55 warnings / 3 hints**.
- Release gate **PASSED** on the signed 0.2.1 pair; install/launch smoke on an API 36 emulator with an empty crash
  log. The design editor also recovered a text-plus-rectangle draft after `am kill` and relaunch; photo and AI
  drafts were not exercised on a device at this baseline.

### Known limits

- **No instrumented tests yet** and no physical-device run.
- Video drafts and video process-death recovery are not covered in 0.2.1; video recovery arrives in 0.2.2 as a
  saved project.
- Model download still restarts from zero after a failure (no resume).
- On Android 8.0-9.0, exports still land in the app's own external folder and are shared through the app's
  `FileProvider` rather than being indexed in the gallery.
- Arabic-Indic numeral rendering is still inconsistent on some screens.
- Exports remain flattened (no camera EXIF or original colour profile); "Largest" stays capped at 3200 px.

## 0.2.0 - 2026-11-18

First signed release baseline, versionCode 2 (closed baseline).

### Added

- **Signed release artifacts**: a release APK and AAB signed with the project upload key, minified with R8 and
  resource-shrunk, with a single minified `classes.dex` and an R8 mapping file produced for the build.
- **16 KB page-size support**: all nine native libraries across the three ABIs are ELF-aligned at `0x4000`, and the
  release APK passes `zipalign -c -P 16`.
- A release-check script gate (`tools/release-check.ps1`) plus a 16 KB check helper (`tools/check_16kb.py`).

### Changed

- Platform baseline moved to **compileSdk/targetSdk 36** (Android 16), minSdk 26 unchanged, on AGP 8.10.1 /
  Gradle 8.11.1 / Kotlin 2.0.21 with Java 17 bytecode.
- Inference runtime moved from TensorFlow Lite 2.16.1 to **LiteRT 2.3.2** (both Apache-2.0); the .tflite models
  are unchanged and run as-is.
- Permissions reduced to **`INTERNET` only**: media/storage permissions and the foreground-service, wake-lock,
  boot and network-state permissions merged in by libraries are explicitly removed. Media is read through the
  Android Photo Picker, exports are written through MediaStore, and legacy sharing uses a non-exported
  `FileProvider`.
- Android backup and device transfer are **disabled** (`allowBackup="false"` plus extraction rules that exclude
  every data domain).
- One AAB carries all three ABIs (arm64-v8a, armeabi-v7a, x86_64) instead of ABI splits.

### Fixed

- Dead-resource and semantics fixes are included in the rebuilt 0.2.0 artifact.

### Verification

- **36 tests in six suites**, 0 failures; `lintDebug` **0 errors / 51 warnings / 3 hints** (the warnings were not
  yet triaged and `lintRelease` had not been run at this baseline).
- Release gate **PASSED**; emulator-only checks on API 26/28/29/36 (no physical device).

### Known limits

- No physical-device run, no instrumented tests, and `lintRelease` had not been run at this baseline.
- A later review of this baseline found process-death data loss, a silently capped 3200 px "Original" export,
  orphan `IS_PENDING` MediaStore rows and untyped export failures. All four were addressed in 0.2.1; do not treat
  0.2.0 as ready for production use.

## 0.1.0 - 2026-11-17

Internal development baseline, versionCode 1. **Never released or distributed.**

### Added

- First complete internal assembly of the app in one Kotlin + Jetpack Compose codebase: photo editor, collage and
  design studio, video editor and on-device AI cut-out, on the 0.1.0 draft platform (minSdk 26, TensorFlow Lite
  2.16.1, Media3 1.5.1). The platform was raised to compileSdk/targetSdk 36 in 0.2.0.
- Debug APK built and statically checked as the baseline for an internal code and product review.

### Notes

- No signing configuration existed, so the release bundle produced from this tree was unsigned; the debug APK was
  the only artifact.
- Superseded by 0.2.0. The review found defects that were fixed in 0.2.1; 0.1.0 must not be built from for
  distribution.

[0.2.3]: https://github.com/ahmadmhmdsy/midad-studio/releases/tag/v0.2.3
