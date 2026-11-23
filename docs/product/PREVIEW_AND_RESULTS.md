# Exact video preview and export result surfaces (0.2.3)

**Status:** implemented in **0.2.3** (versionCode 5) and verified on an API 36 emulator (`emulator-5564`); **no
physical-device verification exists**. The app's permission and network posture is unchanged: one permission
(INTERNET), no ads, no analytics, no accounts.

**Scope of this file:** the two user-facing changes introduced in 0.2.3 — (1) the **full-project video
preview** that plays the exact composition the exporter writes, and (2) the **shared export result dialog**
that now appears automatically for every studio. Related documentation: [PROJECTS.md](PROJECTS.md) and
[../qa/BUILD_REPORT.md](../qa/BUILD_REPORT.md).

---

## 1. Full-project video preview

### 1.1 What it is

Before 0.2.3, the Video studio could preview one selected clip with its trim and colour look. It could not show
what the finished composition would look like — clip order, per-clip trims, frame ratio, title placement and mute
state together. 0.2.3 adds **Preview full project**: a full-screen playback of the same Media3 `Composition` the
exporter writes.

### 1.2 How the guarantee is built

There is exactly one place where a video project becomes a composition:
[`VideoComposition.kt`](../../app/src/main/java/com/midad/studio/studio/video/VideoComposition.kt#L17) —
`VideoComposition.build(spec, titleBitmap)`. Both the exporter and the preview call it.

| Step | Exporter | Full preview |
|---|---|---|
| Composition construction | `VideoComposition.build(spec, titleBitmap)` | the **same function**, same `VideoExportSpec` |
| Per-clip trim | `EditedMediaItem` start/end in the composition | same composition |
| Colour look / frame / title | `VideoEffects.lookEffects` / `frameEffects` applied per item | same effects list |
| Mute | `setRemoveAudio(spec.muted)` | same flag; player volume also forced to 0 when muted |
| Renderer | Media3 Transformer | Media3 `CompositionPlayer` (Media3 **1.5.1**, pinned) |

The preview dialog
([`VideoFullPreviewDialog.kt`](../../app/src/main/java/com/midad/studio/studio/video/VideoFullPreviewDialog.kt))
builds the composition on a background dispatcher, creates a `CompositionPlayer`, calls `setComposition(composition)`
and `prepare()`, then plays when ready. The code comment above `VideoComposition` states the invariant: what the
preview plays is the same sequence, trim, look, output frame and title overlay the exporter writes.

**Why the preview is a scaled proxy, not a re-export.** The composition is rebuilt for playback with a
**960 px long-edge** cap (`VideoFullPreviewDialog.kt:60`, `forPreview()` at line 331). The exporter is untouched and
keeps the project's real output size. The cap exists so preview does not allocate full-resolution composition
buffers; it is the only intentional difference between preview and export.

### 1.3 Controls and lifecycle

| Element | Behaviour |
|---|---|
| Title / subtitle | "Export preview" plus the clip count and total duration (`v_full_preview_title`, `v_full_preview_subtitle_fmt`) |
| Note | "This is the exact result the export will write: clip order and trims, colour look, output frame, mute state and title." (`v_full_preview_note`) |
| Play / pause | Toggle on the player surface; state read from `Player.Listener.onIsPlayingChanged` |
| Seek | Slider over the full duration; position polled (120 ms playing / 400 ms paused) and clamped to the duration |
| Time display | Elapsed / total, formatted by the app's duration formatter |
| Close | "Back to editing" (`v_full_preview_done`), system back, or dialog dismiss |
| Backgrounding | Pauses on `Lifecycle.Event.ON_STOP`; the player and title bitmap are released/disposed, and the dialog does not auto-resume |
| Failure | "The full preview could not be prepared on this device. Export may still work." (`v_full_preview_unavailable`) — a preview failure never blocks export |

**Entry points (two):**

1. **Preview** in the video export bar (`v_preview`) — the existing per-clip preview action.
2. **Preview full project** in the clip preview panel (`v_preview_full_action`), which opens the full-project dialog.

**While exporting:** the preview controls are disabled so the composition cannot be built concurrently with an
export; the export screen itself is unchanged.

### 1.4 Honest limitations

- **Unstable API in a pinned dependency.** `CompositionPlayer` is restricted to the Media3 library group in 1.5.1
  and is annotated `@RestrictTo`; the call site carries an intentional `@SuppressLint("RestrictedApi")`. Media3 is
  pinned at **1.5.1** for the first release; a version bump must be re-tested before it is trusted, and the API may
  change or disappear in a later Media3 release. The exporter itself never uses `CompositionPlayer`.
- **Scaled proxy.** Playback is built at a 960 px long edge, so fine detail and text antialiasing can differ
  slightly from a full-resolution export even though the composition is the same.
- **Codec support matches export, not every source.** Playback uses the device's decoder for the same media the
  export uses; a clip that cannot be decoded may fail in both paths. This preview is not a validation pass.
- **No physical-device verification.** Verified on one API 36 emulator only (see §3). Playback timing and
  hardware-decoder behaviour on real devices are unproven.
- **No audio-level editing.** The preview reflects the project's mute flag only; there is no volume UI in the
  project model.

### 1.5 Verification

- `VideoCompositionTest` covers the composition builder: clip count/order, per-clip trim durations, muting,
  frame/look effects wiring and title overlay presence ([code](../../app/src/test/java/com/midad/studio/studio/video/VideoCompositionTest.kt)).
- Emulator smoke (**2026-11-22**, emulator-5564, API 36, signed 0.2.3 pair built at 20:36): a two-clip project (0:20) opened
  **Preview full project**, played, paused and seeked with the slider; total duration and time display matched the
  project. Evidence images are in the build report's addendum.

---

## 2. Export result dialog (all studios)

### 2.1 What changed

Every studio now shows the **same success dialog** immediately after an export finishes:
Photo (JPG/PNG), Design/collage (PNG/JPG), AI cut-out (PNG) and Video (MP4). It is drawn by one shared component,
[`ExportResultCard.kt`](../../app/src/main/java/com/midad/studio/ui/components/ExportResultCard.kt) —
`ExportSuccessDialog` wraps `ExportResultCard` — instead of the per-studio dialogs that previously showed only a
message (some of them on a default Material 3 surface). The surrounding export flow, options screen and export
index are unchanged.

### 2.2 What the dialog shows

| Element | Behaviour |
|---|---|
| Thumbnail | Loaded from the finished file: images decoded from the exported file, video frame via `MediaMetadataRetriever`. Images sit on a **checkerboard** so transparency is visible; video gets a **▶ badge**. A failed load shows "No preview available" (`export_result_no_thumbnail`). |
| File name | Exact exported name (e.g. `Midad_20261122_204907.mp4`); used in the preview accessibility label (`export_result_preview_fmt`) |
| Location | "Saved in **Pictures/Midad**." for gallery photos, "Saved in **Movies/Midad**." for gallery videos, or "Saved in Midad storage." when the export stayed in app storage (`export_result_location_gallery_fmt`, `export_result_location_app`) |
| **Open** | `ACTION_VIEW` on the exported content URI with a **read grant** (so the viewer can open a file that is not yet indexed); if no viewer handles it, a Toast explains instead of crashing |
| **Share** | Existing sharing path via the app's `FileProvider`; no new storage permission |
| **Close** | Dismisses the dialog and leaves the editor as it was — it does not discard the project or the draft |

The thumbnail itself is clickable and opens the file, matching the Open button.

### 2.3 Honest limitations

- **The location line describes where the app wrote the file, not a guarantee that a gallery app has indexed it
  yet.** On Android 10+ the export goes through MediaStore into Pictures/Midad or Movies/Midad; on the legacy
  API 26–28 path the file lands in the app's own external folder and is reported as app storage (the existing
  storage behaviour that predates 0.2.3).
- **Open depends on another app.** If the device has no viewer that accepts the MIME type/content URI, the button
  shows a Toast; it does not bundle a player or a gallery.
- **Video thumbnails come from the first decodable frame** and can be null for an unusual codec; the dialog then
  shows the no-preview state. It is not a scrubber.
- **No in-dialog retry/share-to-app picker.** Share uses the system chooser; there is no custom target list.
- **Not device-verified.** Verified on emulators only; no physical-device UI pass exists.

### 2.4 Verification

Emulator acceptance (**2026-11-22**, API 36):

- Photo export → dialog with a thumbnail, `Midad_20261122_203900.jpg`, "Saved in Pictures/Midad.", and
  Open / Share / Close.
- Video export → dialog with a frame thumbnail and play mark, `Midad_20261122_204907.mp4`,
  "Saved in Movies/Midad.", and Open / Share / Close.
- Signing release build: home, model manager, full-project preview and video export smoke-tested on the same tree.

---

## 3. Non-goals (deliberately absent)

- No timeline scrubbing on the home screen, no background playback, no picture-in-picture, no preview export.
- No preview of a *different* project than the one open; the dialog previews the current project only.
- No claim that preview equals export pixel-for-pixel: it is the same composition at a scaled proxy.
- No new permissions, SDKs, analytics, ads or network endpoints in either feature. The only outbound requests in
  the app remain the two user-initiated model-file downloads (see
  [DATA_SAFETY.md](DATA_SAFETY.md) and [MODEL_CATALOGUE.md](../research/MODEL_CATALOGUE.md)).

---

## 4. Where the code lives

| File | Role |
|---|---|
| [VideoComposition.kt](../../app/src/main/java/com/midad/studio/studio/video/VideoComposition.kt) | Single composition builder shared by exporter and preview |
| [VideoFullPreviewDialog.kt](../../app/src/main/java/com/midad/studio/studio/video/VideoFullPreviewDialog.kt) | Full-screen `CompositionPlayer` preview, controls, lifecycle, 960 px proxy |
| [VideoStudioScreen.kt](../../app/src/main/java/com/midad/studio/studio/video/VideoStudioScreen.kt) | Preview entry points, export bar, disabled-while-exporting state |
| [ExportResultCard.kt](../../app/src/main/java/com/midad/studio/ui/components/ExportResultCard.kt) | Shared success dialog, thumbnail loading, Open/Share/Close |
| [ExportManager.kt](../../app/src/main/java/com/midad/studio/core/export/ExportManager.kt) | Export destination, MediaStore insert, content URI handed to the dialog |
| [VideoCompositionTest.kt](../../app/src/test/java/com/midad/studio/studio/video/VideoCompositionTest.kt) | Composition builder unit tests |

---

## 5. Open items for later cycles

1. **Physical-device pass** for preview playback and Open/Share on a real device.
2. **Media3 upgrade review** — re-test both preview and export if the pinned 1.5.1 is ever bumped, and check
   whether a stable composition-preview API replaces `CompositionPlayer`.
3. **Preview resolution** — if low-RAM devices stutter, the 960 px cap may need to become device-dependent; measure
   before changing it.
4. **Design/AI dialog theme** — the result dialog previously used the default Material 3 surface; the shared card is
   on the app's palette, but no systematic contrast/TalkBack audit of the new dialog exists yet.
5. **Screenshot recapture** — the store set is still the 0.2.0 set; the full-project preview and the export result
   dialog are new frames worth capturing from a final build (see [../store/README.md](../store/README.md)).
