# Build report — Midad Studio (0.2.0 baseline · 0.2.1 addendum · 0.2.2 addendum · 0.2.3 addendum)

**Report date:** 2026-11-17, 0.2.1 addendum 2026-11-19 19:32, 0.2.2 addendum 2026-11-22 00:16, 0.2.3 addendum 2026-11-22 **18:34 UTC (final build)** · **App:** `com.midad.studio` · **current build versionName 0.2.3** · **versionCode 5** (closed baselines: 0.2.2 / 4, 0.2.1 / 3, 0.2.0 / 2)
**Scope:** the build and verification evidence that exists on disk. The first artifacts below were produced on
2026-11-17: an initial release build at **18:13**, then the final build at **18:23–18:27** after the optional
in-app hosted-policy link was added (it stays hidden while its URL is empty). Tests and `lintDebug` ran inside
those builds, and the artifacts were re-checked afterwards with `tools/release-check.ps1` and Google's
**bundletool 1.18.1**. **No physical-device run has happened**; emulator evidence is recorded in §0.3. Numbers
are read from the files themselves; anything that could not be verified is marked **UNVERIFIED**.
The **0.2.3 build** at the top (§0) is the final build from 2026-11-22 **18:34 UTC**; §0.1 records its
artifact sizes and hashes and §0.2 its tests and lint. §0A is the closed 0.2.2 baseline; §0B is the closed 0.2.1
baseline.

---

## 0. 0.2.3 addendum (2026-11-22) — current build

The 0.2.2, 0.2.1 and 0.2.0 addenda below are kept as closed baselines. This addendum supersedes them for the
current build; the toolchain in §1 is unchanged. The **final 0.2.3 pair was rebuilt at 18:34 UTC** after the
app licence `NOTICE.txt` and the in-app `LicensesScreen` were refreshed (stale 4-model / TensorFlow-Lite naming),
and `tools/release-check.ps1` **PASSED** on it. The tables below are the final numbers, read from the rebuilt files.

### 0.1 Artifacts — final 0.2.3 build (18:34 UTC)

| Artifact | Size | SHA-256 | Built |
|---|---|---|---|
| `app/build/outputs/apk/release/app-release.apk` | **22,072,532 B** (21.05 MiB) | `AA7674D51EB1A966A9714B14DFEA635DB8DA079D88E84E159FE70D8B4D2FA76D` | 2026-11-22 18:34:22 UTC |
| `app/build/outputs/bundle/release/app-release.aab` | **15,480,566 B** (14.76 MiB) | `5BF7B419DB0370ABE34F3E7091BB083BE246C2CA14B1DD0B04C77BEDD17EF903` | 2026-11-22 18:34:24 UTC |
| `app/build/outputs/mapping/release/mapping.txt` | **52,985,913 B** (50.53 MiB) | `D5EA6533029B290F5B511C54F554C5360B64A168BFBD49E982F8A5B420DF2770` | 2026-11-22 18:34:14 UTC |
| single minified `classes.dex` in the release APK | **4,954,744 B** | — | same build |

Use the bytes under `app/build/outputs/apk/release/` and `app/build/outputs/bundle/release/` as the release
evidence; the hashes above are of those files.

Both binaries are signed with the same upload key as 0.2.2/0.2.1: certificate SHA-256
`ee102b16af82833bdfca02edf0efda9757575d1c9f20b0cbe2dc8e7629759c33` (`CN=Midad Studio`, `O=Midad Studio`,
`L=Baghdad`, `C=IQ`). `tools/release-check.ps1` on the 18:34 UTC pair: **RELEASE GATE PASSED** — versionCode **5** /
versionName **0.2.3**, upload-key signature verifies on the APK and the AAB, targetSdk **36** / minSdk **26**,
only `INTERNET` + the AndroidX self-permission, locales default + `ar`, not debuggable, **9 native libraries at
`p_align = 0x4000`** (16 KB ELF aligned). The `mapping.txt` is on disk only: it is not committed to the
repository (size) and is regenerated on every build.

**New legal assets embedded in this build:** `assets/models/LiteRT-LICENSE.txt` (13,575 B, SHA-256
`71c6915d04265772a0339bed47276942c678b45cc01534210ebe6984fd1aec65`),
`assets/models/LiteRT-THIRD_PARTY_NOTICE.txt` (1,960,672 B, SHA-256
`db6e1ff4b16c11eb01c45f135928527a50a05282739054686793ec70f442f119`) and the refreshed
`NOTICE.txt` (6,512 B, SHA-256 `2fe0d5f03c56b49cdf28990417b350cfc6b3be75c139f74b86c3f4708bea8727`).
The model `manifest.json` is now 23,925 B (SHA-256 `9fde946089e2da0e289977aba67af68cb2311b4ee855a2c9268b7fd4f09f0d92`).

### 0.2 Tests and static analysis

- **94 JVM/Robolectric tests in 20 suites, 0 failures, 0 errors, 0 skipped**
  (`:app:testDebugUnitTest`, `forkEvery=1`), measured on the final 0.2.3 tree.
- **Instrumented: 10/10 green** — the `androidTest` suite ran **5/5 on each of two API 36 emulators**, 0
  failures/errors/skips (`:app:connectedDebugAndroidTest`).
- **`lintRelease`: 0 errors / 62 warnings / 3 hints** (`:app:lintRelease`). All 62 warnings are triaged by
  category and none is release-blocking:
  **29** UseKtx, **21** GradleDependency (dependency/version-update notices), **3** StaticFieldLeak false
  positives (application context retained deliberately), **2** MonochromeLauncherIcon, **2** deliberate
  ScopedStorage manifest entries, **1** AGP/Gradle upgrade notice, **1** ModifierParameter, **1** ObsoleteSdkInt
  (`mipmap-anydpi-v26`), **1** RedundantLabel, **1** UnusedAttribute (`localeConfig`). Four dead strings were
  deleted in this pass rather than suppressed. Deferred cleanups are follow-up work, not release blockers.

### 0.3 Emulator acceptance (API 36)

Performed on `emulator-5564` (API 36) with the signed 0.2.3 pair built at **20:36**, plus a debug build of the same
tree for the capture-only flows; the final 18:34 UTC rebuild was smoke-checked only for the licence viewer. This is
**emulator evidence, not a physical-device run**.

- **Photo export**: export succeeded and the shared result dialog appeared — thumbnail, `Midad_20261122_203900.jpg`,
  `Saved in Pictures/Midad.`, Open / Share / Close.
- **AI Lab / model manager**: six model cards shown. MODNet (**≈25 MB**) downloaded, installed and exposed **Try**;
  Try opened Background removal with MODNet selected (chip: "Soft portrait matting (MODNet)") and produced a soft
  hair-edge cut-out on a portrait; the bundled model still ran inference in the signed release.
- **Video full-project preview**: a two-clip project (total 0:20) opened **Preview full project**, played, paused
  and seeked with the slider; duration and time display matched the project.
- **Video export**: export completed and showed the shared dialog — frame thumbnail with play mark,
  `Midad_20261122_204907.mp4`, `Saved in Movies/Midad.`, Open / Share / Close.
- **Release smoke**: Home, model manager, full-project preview and video export launched and ran on the signed
  release build; no crash in `logcat` during the run.

On the **final 18:34 UTC rebuild** the licence viewer was opened on-device and rendered the 1.9 MB third-party
notice. The other flows above were not re-run on that rebuild.

### 0.4 What changed

- **Exact full-project video preview** — `VideoFullPreviewDialog` builds the same Media3
  `Composition` (`VideoComposition.build(spec, titleBitmap)`) as the exporter and plays it with
  `CompositionPlayer` (Media3 **1.5.1**, `@RestrictTo` API, intentional `@SuppressLint("RestrictedApi")`);
  playback is a **960 px long-edge** proxy, export output is unchanged. Entry points: **Preview** in the export
  bar and **Preview full project** in the clip preview panel; the dialog pauses on `ON_STOP`, releases its
  player, and is disabled while an export is active.
- **Shared export result dialog** — one component (`ui/components/ExportResultCard.kt`,
  `ExportSuccessDialog`) for Photo, Design/collage, AI cut-out and Video: thumbnail (checkerboard behind
  transparency; video frame + play mark), file name, save location (`Pictures/Midad`, `Movies/Midad`, or app
  storage), and **Open / Share / Close**. Open uses `ACTION_VIEW` with a read grant and Toast fallback; Share
  keeps the existing `FileProvider` path. Export screens and the export index are unchanged.
- **Model catalogue 4 → 6** — new **DeepLab-V3** (2,780,176 B, SHA-256 `ff36e24d…`, NHWC `[1,257,257,3]` →
  21-class logits with softmax in-app; general objects/pets/products/scenery, Apache-2.0) and **MODNet** portrait
  matting (26,166,776 B, SHA-256 `f7d1e9ec…`, NCHW `[1,3,512,512]` → `[1,1,512,512]` alpha, graph ends
  LOGISTIC so activation is `NONE`; portraits with soft hair edges, Apache-2.0). Both are download-only and
  checksum-verified. Every installed model (bundled or downloaded) now has **Try**, which opens Background
  removal with that model selected; **Remove** stays for every model; the picker shows name and description; the
  automatic choice prefers Selfie Multiclass, MODNet, Selfie square, Selfie landscape, then DeepLab, and an
  explicit choice always wins. MediaPipe models come from `storage.googleapis.com`; the MODNet conversion from
  `huggingface.co`. Those are plain model-file hosts: the network use is still only the user-initiated download,
  with no background request and no other endpoint.
- **Runtime: NCHW support** — the LiteRT bridge accepts NHWC and NCHW 4-D float32 inputs; single-channel NCHW is
  read row-major (planar), multi-channel NCHW is transposed to pixel-major, and each model's layout/activation is
  declared in the manifest instead of assumed. No new permission, SDK, analytics, ad code or account.
- **Licence surfaces** — refreshed in the final build. `assets/models/NOTICE.txt` is **6,512 B** (SHA-256
  `2fe0d5f03c56b49cdf28990417b350cfc6b3be75c139f74b86c3f4708bea8727`): LiteRT runtime notice, DeepLab HF
  commit / no-card statement, MODNet conversion / modification statement, network + storage disclosure and an
  intended-use line, as a self-contained notice. The build embeds **LiteRT-LICENSE.txt** (13,575 B, SHA-256
  `71c6915d04265772a0339bed47276942c678b45cc01534210ebe6984fd1aec65`) and
  **LiteRT-THIRD_PARTY_NOTICE.txt** (1,960,672 B, SHA-256
  `db6e1ff4b16c11eb01c45f135928527a50a05282739054686793ec70f442f119`). `LicensesScreen` now opens each entry
  (LiteRT licence, LiteRT third-party attributions, model credits / NOTICE, the Apache-2.0 text, every model
  row) in an in-app viewer of the bundled text, with EN/AR copy reworded to "open an entry to read its licence".
  The manifest pins the MODNet source and download URLs to HF commit
  `4d5bd22bde15c8ae5e215ca4799513844979a431` (attribution author "Qiong Yan"; `latestAliasUrl` stays `main`
  intentionally). Licence evidence remains in [../product/MODEL_LICENSES.md](../product/MODEL_LICENSES.md) and
  [../research/MODEL_CATALOGUE.md](../research/MODEL_CATALOGUE.md).

### 0.5 What this build does not claim

- **No physical-device QA.** Emulator only; a real-device run has not been done.
- **No final-pair physical QA.** Artifact numbers in §0.1 are final for the 18:34 UTC build and
  are read from the files under `app/build/outputs/`.
- **No claim that the mapping file is published** — it is on disk and not committed.
- **No MODNet performance benchmark** was run on-device or in-app; the download/install/Try flow was verified, not
  speed or memory. The MODNet download is **≈25 MB** on a metered connection.
- **No quality claim beyond each model's scope** — DeepLab-V3 is a general segmenter (objects, pets, products,
  scenery) and is **not** a portrait model; MODNet is **portraits only**. Neither is benchmarked for edge quality.
- **No claim of pixel-identical preview** — the full preview is the same composition at a 960 px proxy.
- **Not distributed through Google Play.** The app is not currently published on Google Play; store metadata
  and declarations are tracked separately.

---

## 0A. 0.2.2 addendum (2026-11-22 00:16) — closed baseline (superseded by 0.2.3)

These 0.2.2 numbers are retained as history; the current build is 0.2.3 (§0). The 0.2.1 and
0.2.0 sections below remain closed baselines; the toolchain in §1 is unchanged.

### 0A.1 Artifacts

| Artifact | Size | SHA-256 | Built |
|---|---|---|---|
| app/build/outputs/apk/release/app-release.apk | **21,808,278 B** (20.80 MiB) | `E99F427CEF2EB3461B15CE310EA1452D77F82E7285D84EE4BDD2265C492F51A6` | 2026-11-22 00:16 |
| app/build/outputs/bundle/release/app-release.aab | **15,186,220 B** (14.48 MiB) | `AA667BFD2EC2985BBD510F6D14CB6202FB19D191AD75730AC5C010A7E4EB4137` | 2026-11-22 00:16 |
| app/build/outputs/mapping/release/mapping.txt | **52,270,179 B** (49.85 MiB) | `C24A63DEE669F6EF838296562C0BC9B7C1E719CC04009125701CFCAF06EC73FD` | 2026-11-22 00:16 |

Both binaries are signed with the same upload key as 0.2.1 (certificate SHA-256 `EE:10:2B:16:…:9C:33`).
`tools/release-check.ps1` on this exact pair: **PASSED** — versionCode 4 / versionName 0.2.2, upload-key
signature verifies on the APK and the AAB, targetSdk 36 / minSdk 26, only `INTERNET` + the AndroidX
self-permission, locales default + `ar`, three ABIs, and all 9 native libraries at `p_align = 0x4000`.
Keep `mapping.txt` with this AAB hash: it is the only deobfuscation file for it, and a rebuild overwrites it.
The 0.2.1 pair (§0B) and the 0.2.0 pair (§2) predate the projects work; their numbers are history.

### 0A.2 Tests and static analysis

- **90 JVM/Robolectric tests across 18 suites, 0 failures, 0 errors, 0 skipped**
  (app/build/test-results/testDebugUnitTest/, 18 XML files read 2026-11-22). The suite now includes
  `ProjectRepositoryTest`, `VideoProjectCodecTest` and `VideoDraftStoreTest` for the projects work, plus
  `ModelResumeTest`, `FormattersTest`, `EditorAccessibilityTest`, `ScreenshotRenderTest` and
  `DesignTemplatesMirrorTest`; the 0.2.1 suites still pass.
- **`lintRelease`: 0 errors, 65 warnings, 3 hints** (app/build/reports/lint-results-release.txt, written
  2026-11-22 00:13); `lintVitalRelease` passes inside the signed release build. **All 65 warnings are triaged
  by category; none is a release blocker:**
  - **29 `UseKtx`** — style-only KTX suggestions (createBitmap/scale/toUri/withRotation/…). Deferred to a
    dedicated cleanup change; touching 20+ files after the artifact is verified would invalidate the build.
  - **21 toolchain/dependency** (20 `GradleDependency` + 1 `AndroidGradlePluginVersion`) — Compose BOM,
    Media3, LiteRT, Lifecycle, Navigation, AGP/Gradle upgrades. Follow-up maintenance; each needs a full
    regression, and the pinned versions stay for 0.2.2.
  - **3 `StaticFieldLeak`** — inspected: all three fields hold `applicationContext`, not an Activity, for
    draft/project storage (`DesignViewModel`, `PhotoEditorViewModel`, `VideoStudioViewModel`). False
    positive; no Activity context is retained.
  - **3 `UnusedResources`** — three strings left dead by the Projects/Exports split
    (`projects_empty_action`, `p_export_failed`, `d_export_failed`); safe cleanup for the next patch,
    not release-blocking.
  - **2 `MonochromeLauncherIcon`** — adaptive icons lack `<monochrome>`, so themed icons fall back to the
    normal icon on Android 13+; cosmetic, fix with the next icon pass.
  - **2 `ScopedStorage`** — deliberate `tools:node="remove"` lines for `READ/WRITE_EXTERNAL_STORAGE`
    that keep the one-permission claim; expected.
  - **5 low-risk singles** — `RedundantLabel` (activity label), `UnusedAttribute` (`localeConfig` below
    API 33), `ObsoleteSdkInt` (`mipmap-anydpi-v26` with minSdk 26), `PluralsCandidate`
    (`v_clip_count_fmt`), `ModifierParameter` (`EditorControls`). Accepted for this release.
- **Instrumented tests pass 5/5 on each of two API 36 emulators (10/10 total, 0 failures, 0 errors, 0
  skipped)** — `review36b(AVD)` and `sighaqa36(AVD)`, connected run 2026-11-22 00:14, result XMLs under
  app/build/outputs/androidTest-results/connected/. The classes are `ManifestPermissionsInstrumentedTest`,
  `ExportManagerInstrumentedTest`, `PhotoDraftStoreInstrumentedTest` and
  `ModelStorageInstrumentedTest`.
- **No real device** ran this build. Emulator is the current acceptance path; real-device QA has not been run.

### 0A.3 Device check

The signed 0.2.2 release APK was installed on `emulator-5554` and `emulator-5558` (both API 36), launched to
the Home screen, and used for projects smoke tests. On `emulator-5554`: Home → Projects → New project →
Design → Square post → save "Design project" → back → Projects showed the card → reopen loaded the editor
with the title **Design project**; the Video editor's trim range rendered as `0:00 – 0:03` instead of raw
milliseconds. On `emulator-5558` (final build): AI Lab → Background removal → choose a public-domain
portrait with a real subject → transparent cut-out → save "Cut-out project" → force-stop → relaunch → Projects →
open the project (confirming the unfinished-draft dialog) → the toolbar shows **Cut-out project** and the saved
cut-out renders on its transparent checkerboard (captured locally); the card thumbnail shows the
same image. A debug build from the same tree exercised more surface
before the final build: Design and Photo projects (create → save → list → reopen; rename; delete), a Video
project (create → save → list → reopen with its clip and trim intact). The AI project-title bug found by that
smoke (draft restore raced `openProject` and left the toolbar generic) was fixed and re-verified on the
final release. The remaining unexercised UI path is a deliberately corrupted project payload; the repository
handles it in unit tests. The 0.2.1 launch smoke in §0B.3 below is history only.

### 0A.4 What changed

- **Projects (new):** create/save/reopen/rename/delete an editable session in Photo, Design, AI cut-out and
  Video; thumbnail grid; atomically replaced versioned index under `noBackupFilesDir/projects`; no sync,
  sharing or import/export of project files. Architecture and guarantees:
  [docs/product/PROJECTS.md](../product/PROJECTS.md).
- **Video projects:** versioned JSON payload with persistable URI grants (no copied video bytes), a
  **3-minute total / 5-clip cap** enforced on load and when adding clips, with visible notices instead of
  silent truncation; unknown enum values fall back safely.
- **Draft versus project:** drafts remain the process-death recovery path; projects are explicit saves. On
  explicit exit with a bound project the newest edit is flushed to the project, then the draft is cleared; if
  the flush fails the draft is kept so the edit is not lost. Opening a project while an unfinished draft exists
  asks before replacing it, and a bound video project leaves without a false "your work will be lost" prompt
  because the exit path flushes it first.
- **Reachability and labels:** a Home card opens Projects/Exports; saved project titles appear in the Photo,
  Design, Collage, AI cut-out and Video toolbars instead of the generic studio title; long-press on a card
  exposes rename/delete with localized labels; the Video trim range is formatted as minutes:seconds.
- **Video URI grants:** clip content URIs are persisted with `takePersistableUriPermission` when added and
  when a payload is restored, so a reopened project can still decode its clips after process death.
- **AI project restore:** draft recovery now starts from the screen after the route is known, instead of from
  `AiViewModel.init`, so opening a saved cut-out project binds its title and id deterministically; the race
  was found by emulator smoke and is covered by the manual re-run above.
- **Arabic and numeric polish:** `a_tool_models_ready` now has real Arabic `zero/one/two/few/many/other`
  plurals; slider, percentage, brush-size and export-quality values follow the locale (MidadNumbers);
  number/plural tests extended.
- **Export metadata note:** the photo export surface now states that exports are flattened images — camera
  EXIF data and the original colour profile are not carried over (`p_export_metadata_note`, EN + AR).
- **Unchanged guarantees:** no ads, analytics or tracking SDK; no new permissions (`INTERNET` only); no data
  collection. Monetization remains a planning document only.

---
## 0B. 0.2.1 addendum (2026-11-19) — closed baseline

The 0.2.0 sections below are kept as the closed-test baseline; the 0.2.3 addendum above is the current one.
This 0.2.1 addendum is retained for the 0.2.1 artifact and the toolchain, which is unchanged.

### 0B.1 Artifacts

| Artifact | Size | SHA-256 | Built |
|---|---|---|---|
| `app/build/outputs/apk/release/app-release.apk` | **21,752,502 B** (20.74 MiB) | `B1CE0E179AA012F291157F8F1E6E8741BF1837C3405FD43A2AA5CCF596630984` | 2026-11-19 19:31:59 |
| `app/build/outputs/bundle/release/app-release.aab` | **15,046,715 B** (14.35 MiB) | `DA8F2875E9FAF5FC7BBC1FFB12A8BFE1BC1DA7DAB1A4B30A77BDEEE3A36C162B` | 2026-11-19 19:32:03 |
| `app/build/outputs/mapping/release/mapping.txt` | **51,059,929 B** (48.69 MiB) | `56ECEFCD3B691F262450301152ACCCBCF58BCADF978FB99D249340BF72050786` | 2026-11-19 19:31:47 |

Both binaries are signed with the upload key (`CN=Midad Studio`, certificate SHA-256 `EE:10:2B:16:AF:82:83:3B:DF:CA:02:ED:F0:EF:DA:97:57:57:5D:1C:9F:20:B0:CB:E2:DC:8E:76:29:75:9C:33`).
`tools/release-check.ps1` on this exact pair: **PASSED** — versionCode 3 / versionName 0.2.1, AAB signature
verifies entry by entry, APK signature verifies, targetSdk 36 / minSdk 26, only `INTERNET` +
the AndroidX self-permission, three ABIs, all 9 native libraries at `p_align = 0x4000`. Badging was re-read with
`aapt2 dump badging`. The 0.2.0 pair in §2 predates the P0 fixes; its numbers are history.

### 0B.2 Tests and static analysis

- **62 JVM/Robolectric tests, 0 failures, 0 errors, 0 skipped** (`app/build/test-results/testDebugUnitTest/`,
  run 2026-11-19 19:27). New in 0.2.1: photo draft codec/store, design draft codec/store, AI draft store,
  export-size planning — 26 new tests; the 36 existing tests still pass.
- **`lintRelease`: 0 errors, 55 warnings, 3 hints** (`app/build/reports/lint-results-release.txt`, 19:27);
  `lintVitalRelease` passed inside the signed build. Warnings are untriaged.
- No instrumented tests in 0.2.1: `app/src/androidTest` did not exist yet. (0.2.2 adds androidTest sources; see the 0.2.2 addendum above.)

### 0B.3 Device check

The signed 0.2.1 APK was installed with `adb install -r` on the API 36 emulator: install succeeded,
`.MainActivity` became `topResumedActivity`, `pidof com.midad.studio` returned a live process and
`logcat -d AndroidRuntime:E` was empty. The home screen rendered in Arabic (capture kept locally).
This is a launch smoke test, **not** a full QA pass. The design draft was exercised on that emulator:
a text layer and a rectangle were added, the app was sent home, `am kill` emptied the process, and relaunching
reopened the design editor with both layers intact; after an explicit back to the design start screen the same
kill/relaunch opened the start screen with no restored editor. Photo and AI drafts were not yet exercised on a
device, and no crash appeared in `logcat` during either run.

### 0B.4 What changed

P0: export outcomes are typed (`NO_SPACE`/`IO`/`ENCODE`/`UNKNOWN`) with free-space checks and pending-row
cleanup; the "Original" chip is now "Largest" with the 3200 px cap disclosed; photo, design/collage and AI cut-out
sessions survive process death via versioned binary drafts with app-owned pixel copies, restored on entry and
deleted on explicit exit. P1: model download gains a disk-space pre-check and `ENOSPC` classification (resume
still not implemented); replaced design images get a fresh layer id so undo cannot hit a recycled bitmap; the video
bar respects the navigation inset; release tasks fail loudly without `keystore.properties`. Video drafts and
instrumented tests remain open.

---

## 1. Toolchain — declared and observed

| Component | Version | Source |
|---|---|---|
| Android Gradle Plugin | **8.10.1** | `build.gradle.kts` (root) |
| Gradle wrapper | **8.11.1** | `gradle/wrapper/gradle-wrapper.properties` |
| Kotlin | **2.0.21** | root plugin declaration |
| Java bytecode target | **17** | `app/build.gradle.kts` (`jvmTarget = "17"`) |
| JDK installed on the build machine | **21.0.13.9** (Eclipse Adoptium, `C:\Program Files\Eclipse Adoptium\jdk-21.0.13.9-hotspot`) | `java`/keytool path used for the checks below |
| compileSdk / targetSdk | **36 / 36** | `app/build.gradle.kts`; confirmed by `aapt2 dump badging` |
| minSdk | **26** | same |
| Compose | **BOM 2024.12.01** (Material 3, UI, foundation); activity-compose 1.9.3; navigation-compose 2.8.5 | `app/build.gradle.kts` |
| Media3 | **1.5.1** (pinned) | `app/build.gradle.kts` |
| LiteRT | **2.3.2** (`com.google.ai.edge.litert:litert`) | `app/build.gradle.kts`; native libs confirmed in both artifacts |
| JUnit | 4.13.2 | `app/build.gradle.kts` |
| ABIs | `arm64-v8a`, `armeabi-v7a`, `x86_64` | `abiFilters`; confirmed by badging |
| Release hardening | R8 minification + resource shrinking | `isMinifyEnabled`/`isShrinkResources`; single minified `classes.dex` (4,750,076 B) in the release APK |

Gradle ran on JDK **21.0.13.9**: `gradlew.bat --version` printed "Launcher JVM: 21.0.13 (Eclipse Adoptium
21.0.13+9-LTS)", and every build command behind this report exported the same `JAVA_HOME`. The bytecode target
remains 17.

---

## 2. Artifacts on disk (measured)

### 2.1 Debug APK — `app/build/outputs/apk/debug/app-debug.apk`

| Property | Value |
|---|---|
| Size | **34,999,713 bytes** (33.38 MiB) |
| Modified | 2026-11-17 **18:20:11** |
| SHA-256 | `CC30E397E98E680401CA2390D3324E88A29E8A57BC4F5B79F63D434686B0BCC3` |
| Signature | debug keystore — for local testing only |
| Badging | versionCode **2**, versionName **0.2.0**, compileSdk **36**, targetSdk **36**, minSdk **26**, ABIs arm64-v8a/armeabi-v7a/x86_64 |
| Permissions | `INTERNET` + `com.midad.studio.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` **only** |

### 2.2 Release APK — `app/build/outputs/apk/release/app-release.apk`

| Property | Value |
|---|---|
| Size | **21,730,768 bytes** (20.72 MiB) |
| Modified | 2026-11-17 **18:27:39** |
| SHA-256 | `B2D1B5DA9D3B975A9F1D49C15483FEEF5EFF5CFB021E8C97D1CF90DA6647B176` |
| Signature | **signed with the upload key**: `CN=Midad Studio, OU=Mobile, O=Midad Studio, L=Baghdad, C=IQ`; certificate SHA-256 `EE:10:2B:16:AF:82:83:3B:DF:CA:02:ED:F0:EF:DA:97:57:57:5D:1C:9F:20:B0:CB:E2:DC:8E:76:29:75:9C:33` — matches the certificate in `keystore/midad-upload.jks` (alias `midad`) |
| Badging | versionCode **2**, versionName **0.2.0**, compileSdk/targetSdk **36**, minSdk **26**, three ABIs |
| Permissions | `INTERNET` + `com.midad.studio.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` **only** |
| Contents | one minified `classes.dex` (4,750,076 B), LiteRT + AndroidX native libs, no `libtensorflowlite_jni.so` |

### 2.3 Android App Bundle — `app/build/outputs/bundle/release/app-release.aab`

| Property | Value |
|---|---|
| Size | **14,986,612 bytes** (14.29 MiB) |
| Modified | 2026-11-17 **18:27:45** |
| SHA-256 | `2DEA6D5B1DA6B4C0C1F26D27604C14FA5018C757859DC39FBE759E340C1D9E8F` |
| Signature | **signed**: `META-INF/MIDAD.RSA` and `META-INF/MIDAD.SF`; certificate `CN=Midad Studio` |
| Contents | `base/lib/{arm64-v8a,armeabi-v7a,x86_64}/libLiteRt.so`, `liblitert_jni.so`, `libandroidx.graphics.path.so`; **no TFLite library** |
| Manifest | **dumped from the bundle** with `bundletool 1.18.1 dump manifest` (validate exit 0): `package="com.midad.studio"`, `versionCode="2"`, `versionName="0.2.0"`, `minSdkVersion="26"`, `targetSdkVersion="36"`, `android:allowBackup="false"`, `android:extractNativeLibs="false"`, `android:localeConfig`, and only `INTERNET` + the AndroidX self-permission. `bundletool build-apks --mode=universal` also succeeded and produced a 21,712,194 B universal APK with identical badging. |
| Universal APK from the AAB | `bundletool build-apks --mode=universal` | **built**: 21,712,194 B, badging identical to §2.2; signed with the local **debug** key by bundletool, so it is for local installation tests only; the AAB is the distributable bundle format. |

### 2.4 R8 output

| Path | State |
|---|---|
| `app/build/outputs/mapping/release/mapping.txt` | **present**, 50,724,171 B (48.37 MiB), 18:27:25 — **archive it with the AAB and its hash**; it is not committed (size, and it is regenerated per build) |
| `app/build/outputs/mapping/release/seeds.txt` (748,583 B) and `usage.txt` (4,693,232 B) | present, 18:23:40 and 18:26:23 |
| `configuration.txt` (62,824 B) and `resources.txt` (292,142 B) | present, 18:27:28 and 18:27:32 |

The single minified `classes.dex`, the mapping and the seeds/usage files prove R8 ran on the **final** tree and
that a crash report from this build can be deobfuscated. The mapping is the only copy tied to this AAB hash;
store it before handing the bundle to anyone.

---

## 3. Tests and static analysis

### 3.1 Unit tests — `app/build/test-results/testDebugUnitTest/`

**18 tests, 0 failures, 0 errors, 0 skipped** (results written 18:20:38; the 18:27 build reported the task
`UP-TO-DATE` because no test input changed after that run).

| Suite | Tests | Result |
|---|---|---|
| `com.midad.studio.core.export.ExportMimeTest` | 4 | pass |
| `com.midad.studio.core.image.BitmapToolsTest` | 6 | pass |
| `com.midad.studio.LocalizationParityTest` | 4 | pass |
| `com.midad.studio.ui.text.MidadNumbersTest` | 4 | pass |

These are JVM tests only. There are **no instrumented tests** and no test touches a device, a codec or a model.

### 3.2 Lint — `app/build/reports/lint-results-debug.txt`

**0 errors, 56 warnings, 3 hints** (final `lintDebug` report written 18:34:40 on the final tree; `lintVitalRelease`
ran inside every release build and found no fatal issue). The warning count rose from 30 before the remediation
work; several are expected `ScopedStorage` warnings pointing at the manifest lines that are deliberately removed
with `tools:node="remove"` (`READ_EXTERNAL_STORAGE`, `WRITE_EXTERNAL_STORAGE`, …), and one is a `UseKtx`
suggestion for the new optional policy link. The set has **not** been triaged item by item, and `lintRelease`
has not been run.

---

## 4. Verification performed on these artifacts

| Check | Method | Result |
|---|---|---|
| Identity | `aapt2 dump badging` on both APKs | versionCode 2, versionName 0.2.0, compileSdk/targetSdk 36, minSdk 26, three ABIs |
| Permissions (debug APK) | `aapt2 dump permissions` | `INTERNET` + AndroidX self-permission only |
| Permissions (release APK) | `aapt2 dump permissions` | `INTERNET` + AndroidX self-permission only |
| Permissions (merged release manifest) | read `intermediates/merged_manifests/release/.../AndroidManifest.xml` | `INTERNET` + AndroidX self-permission only; `allowBackup="false"`; `targetSdkVersion="36"` |
| **AAB manifest** | `bundletool dump manifest --bundle app-release.aab` | `versionCode 2`, `versionName 0.2.0`, minSdk 26, targetSdk 36, `allowBackup="false"`, `extractNativeLibs="false"`, only `INTERNET` + self-permission |
| **AAB validity** | `bundletool validate --bundle app-release.aab` | **exit 0**, no errors or warnings |
| **Release gate** | `pwsh -NoProfile -File tools/release-check.ps1` | **RELEASE GATE: PASSED** — signed AAB/APK, 16 KB ELF alignment (9/9 libs), targetSdk 36, minSdk 26, one user permission, en/ar locales, non-debuggable, three ABIs |
| APK signature scheme | `apksigner verify --print-certs app-release.apk` | verifies; signer certificate SHA-256 `EE:10:2B:16:…:9C:33`, matching the upload keystore |
| Signing | `apksigner verify --print-certs` (APK); `jarsigner -verify` (AAB); `keytool -list -v` on the keystore | APK certificate SHA-256 matches the upload keystore certificate; AAB carries `MIDAD.RSA`/`MIDAD.SF` |
| 16 KB zip alignment | `zipalign -c -P 16 -v 4 app-release.apk` (build-tools 36.0.0) | **Verification successful** (re-run on the final APK) |
| 16 KB ELF alignment | extracted every `.so` from the **release APK** and parsed `PT_LOAD` program headers | **pass** — 9 libraries across three ABIs, every `p_align = 0x4000` |
| Unit tests | `testDebugUnitTest` results | 18/18 pass |
| Lint | `lintDebug` report | 0 errors, 56 warnings, 3 hints |

**Limits of this verification:**

- Nothing was installed or run on a device, emulator or 16 KB-page system. Alignment is proven structurally, not
  behaviourally. The universal APK built by bundletool was not installed either (no ADB target on this machine).
- The AAB and its manifest are verified with bundletool; it has not been through Google Play's own upload
  validation.
- Only the debug variant was linted; `lintRelease` has not run.
- The model download was not exercised against the real host; offline inference was not exercised at all.
- The 56 lint warnings have not been triaged one by one (§3.2).

---

## 5. Truth table — what is proven, what is not

| Claim | Status |
|---|---|
| `versionCode` 2 / `versionName` 0.2.0 in the built artifacts | **Proven** (badging) |
| targetSdk 36 in the built artifacts | **Proven** (badging on both APKs, merged release manifest, and the AAB manifest dumped with bundletool) |
| Only `INTERNET` + the AndroidX self-permission are requested | **Proven on both APKs, the merged release manifest, and the AAB manifest** |
| Backup disabled for the app | **Proven in source, the merged release manifest, and the AAB manifest**; not proven on a device |
| The AAB Play will receive is structurally valid | **Proven** — `bundletool validate` exit 0 and `build-apks` succeeds |
| Release build is signed with the upload key | **Proven** (certificate digest matches keystore) |
| R8 minification ran | **Proven** (single minified dex, seeds/usage) |
| R8 mapping available for this build | **Proven** — `mapping.txt` (50,724,171 B) written 18:27:25 alongside the AAB; keep it with the AAB |
| 16 KB compatible native libraries and zip layout | **Proven for the release APK** (ELF `0x4000`, zipalign pass) |
| Unit tests pass | **Proven** — 18/18 |
| Lint clean (errors) on the current tree | **Proven for `lintDebug`** — 0 errors; 56 warnings untriaged; `lintRelease` not run |
| App runs on a device | **Not proven** — no device QA |
| Model download succeeds against the real host, and offline cut-out works | **Not proven** — no runtime test |
| Release build is installable | **Not proven** — never installed; bundletool produced a universal APK from the AAB, but no device/ADB was available to install it |

---

## 6. Reproduction

```powershell
# JDK 17+ and Android SDK 36 required (this machine: JDK 21.0.13.9)
.\gradlew.bat -version
.\gradlew.bat clean :app:testDebugUnitTest :app:lintDebug
.\gradlew.bat :app:bundleRelease :app:assembleRelease
# outputs: app\build\outputs\bundle\release\app-release.aab
#          app\build\outputs\apk\release\app-release.apk
# archive: app\build\outputs\mapping\release\mapping.txt  (with the AAB hash)
pwsh -NoProfile -File tools\release-check.ps1
```

Optional bundle checks (bundletool is not part of the Android SDK):

```
java -jar bundletool.jar validate --bundle app\build\outputs\bundle\release\app-release.aab
java -jar bundletool.jar dump manifest --bundle app\build\outputs\bundle\release\app-release.aab
java -jar bundletool.jar build-apks --mode=universal --bundle app\build\outputs\bundle\release\app-release.aab --output midad-universal.apks
```

Re-run the checks above and record the new sizes, hashes and dates. The numbers above describe the
**2026-11-17 18:27 final build**; a rebuild invalidates them.

---

## 7. Residual risks and open items

| Risk | Why it matters | Action |
|---|---|---|
| **No physical-device QA** | Photo, video and AI paths are unproven on real hardware; launch crashes are a common quality risk | Test on a low-RAM device and a current flagship |
| **R8 mapping not yet archived off-machine** | Crash reports from the minified build are unreadable without it, and any rebuild overwrites it | Copy `mapping.txt` next to the AAB with the AAB hash; it is not in git by design |
| **`largeHeap="true"` retained** | Masks memory pressure on low-RAM devices | Test on 2–3 GB; keep only with evidence |
| **`-Xskip-metadata-version-check`** | A divergent Kotlin metadata version can fail at runtime, not at build time | Keep dependencies pinned; smoke-test every studio after any dependency change |
| **media3 pinned at 1.5.1** | Transformer/codec behaviour is version-sensitive | No bump before the first release; re-test export after any bump |
| **56 lint warnings untriaged** | Some may be real defects; the `ScopedStorage` ones are intentional | Triage and run `lintRelease` |
| **Arabic-Indic numeral formatting** | Inconsistent number rendering between locales is a known issue | Extend `MidadNumbersTest`; check every number-bearing screen |
| **Accessibility gaps** | Icon-only controls and touch targets are not fully labelled/audited | TalkBack pass on Home and Photo at minimum |
| **Older artifacts from the 18:13 build** | The 18:13 AAB/APK were replaced by the 18:27 build; only the hashes in §2 belong to the final tree | Hand out only files matching §2 |
| **In-app policy link stays hidden until configured** | `settings_privacy_url` ships empty, so the in-app "read the full policy" row does not appear until the hosted policy URL is set and the app rebuilt | Set the URL before a production build; the published policy page alone does not populate the in-app link |

---

## 8. Sign-off for this report

| Question | Answer |
|---|---|
| Was a build run for this report? | **Yes** — the 18:13 build and the 18:23–18:27 final build; the documentation pass itself did not build. |
| Is there a releasable artifact? | **Yes for distribution:** a signed AAB exists, `bundletool validate` exits 0, its manifest matches, the APK passes signature/permission/16 KB checks, and the R8 mapping exists. What remains is the absence of any device run — not artifact quality. |
| Which numbers are measured? | §2 sizes, hashes, timestamps; §3 test and lint counts; §4 checks. |
| Which claims are unverified? | Everything in §5 marked "not proven", plus `lintRelease`, store-side validation, and every runtime path on a device. |
| What closes the gap? | A physical-device test pass, `lintRelease` triage, store metadata and hosting, and store-side validation of the AAB. |

---

## 9. Update 2026-11-19 — 0.2.0/0.2.1 artifact (history; superseded by the 0.2.2 addendum)

The 2026-11-17 report above is retained as history. The artifact described here was rebuilt on **2026-11-18 20:16**
after the dead-resource and semantics fixes, and every number below was re-measured on 2026-11-19.

| Property | Release APK | Release AAB |
|---|---|---|
| Path | app/build/outputs/apk/release/app-release.apk | app/build/outputs/bundle/release/app-release.aab |
| Size | **21,734,997 B** (20.73 MiB) | **15,001,334 B** (14.31 MiB) |
| SHA-256 | `7C10EEC49EE7CAD7F0B7B3A56886E3FB4F5BABA5ABFEA0576DC7CA9729384861` | `1630D4CC24D2058A2B83508B160BE9337FA728BA35945B1D4F6A8B9C1004CFE5` |
| Built | 2026-11-18 20:16:43 | 2026-11-18 20:16:46 |
| R8 mapping | `91F8BC19274E417562EFD005D69035D57C1F421E97F755FE4675E0AFF819DFE3` · **50,776,560 B** (20:16:29) | same file |

| Check | Result |
|---|---|
| `tools/release-check.ps1` | **PASSED** (run 2026-11-18 20:17): version 0.2.0, targetSdk 36, minSdk 26, INTERNET only, locales ar + default, not debuggable, upload-key signature, three ABIs |
| Unit tests | **36 tests, 0 failures** (6 suites, latest run 2026-11-18 20:08) |
| Lint | `lintDebug`: **0 errors / 51 warnings / 3 hints** (report 20:10). `lintRelease` **not run**; warnings untriaged |
| 16 KB page size | **verified** on this APK: `zipalign -c -P 16` passes; all 9 `.so` files report `PT_LOAD p_align = 0x4000` (re-checked 2026-11-19) |
| Store target API | targetSdk 36 meets Google Play's target-API requirement for new apps and updates since **2026-08-31** (Play Help 11926878, fetched 2026-11-19) |
| Device evidence | Emulator only: API 36 visual/flow pass, API 26/28/29 bar/theme/editor pass. **No real hardware.** |

**Independent audit (2026-11-19, read-only).** Four P0 defects are now documented and are not visible in this
artifact's green build gates: process-death data loss (no `SavedStateHandle`/`rememberSaveable`); the
"Original" export chip silently capped at 3200 px (`PhotoExporter.kt:146`, fallback at `:139-141`); possible
orphan `IS_PENDING=1` MediaStore rows on cancellation (`ExportManager.kt:56-85`); and one generic error for every
save failure with no free-space check (`ExportManager.kt:43-54,105-117`). P1 items include the video bar's missing
`navigationBarsPadding()`, model-download resume/error classification, length-only model integrity on the hot
path, a silent unsigned-AAB fallback when `keystore.properties` is missing, and an undo path that can restore a
recycled bitmap. The full list, ordering and acceptance gates are kept in the project's internal review records.
