# Projects — design, storage and behaviour (0.2.2)

**Status:** introduced in 0.2.2 (versionCode 4), built 2026-11-21, and unchanged in 0.2.3. **Build-,
unit-test- and emulator-verified (API 36) for Photo, Design and Video; AI cut-out and corrupt-payload
UI paths are covered by repository tests but not by a UI smoke.** Not tested on a physical device.
This page is the reference for what "Save project" and "Open project" actually do, so later studio
work does not quietly break the guarantees below. Build evidence is in
[BUILD_REPORT.md](../qa/BUILD_REPORT.md).

## 1. What a project is

A project is one editable session saved inside the app, so it can be reopened later instead of
being kept only as a finished export. Every studio can create one:

| Studio | Payload | What is stored |
|---|---|---|
| Photo | the photo draft folder | working pixels (JPEG/PNG), adjustments, crop, text, stickers, strokes, next layer id |
| Design | the design draft folder | document JSON plus one PNG per image layer |
| AI cut-out | the cut-out draft folder | source image and cut-out settings/result metadata |
| Video | versioned JSON | clip content URIs, trim points, look, frame, mute, title |

A project is **not** a portable file and is **not** synced. It lives in
`context.noBackupFilesDir/projects/`, which is excluded from Android Auto Backup and device
transfer (Android backup is disabled for the app; see [DATA_SAFETY.md](DATA_SAFETY.md)). Reinstalling
the app or moving to a new phone does not bring projects back. Exported files in the gallery are unaffected.

## 2. On-disk layout

```
noBackupFilesDir/
  projects/
    index.json                 versioned list: id, kind, title, createdAt, updatedAt, hasThumbnail
    <uuid>/
      payload/                 photo/design/AI draft folder, or video project.json
      thumbnail.jpg            512 px JPEG preview for the grid
```

- IDs are UUID strings validated against `[A-Za-z0-9-]{1,64}` before any file path is built.
- Writes go to a staging directory and are swapped in by rename; a failed write never deletes the
  previous project or the studio draft it was copied from.
- The index is replaced atomically (temp file + rename). `refresh()` drops index rows whose
  directory disappeared, so a damaged entry cannot crash the library.
- Deleting a project removes the index row first, then the directory. A directory left behind by a
  failed index write is an invisible orphan, not a dangling row.

## 3. User flow

1. **Create** — Home → Projects card → *New project* → choose a studio. The studio opens empty.
   The Home card and the Projects screen use a tab argument so "See all" on recent exports opens
   the Exports tab, while the Projects card opens the Projects tab.
2. **Save** — the studio toolbar has a **Save project** action (Design: the **Project** rail item).
   The first save asks for a title (pre-filled with the localized default, e.g. "Photo project");
   later saves update the same project without asking again. A toast confirms success or reports a
   typed failure.
3. **Reopen** — Projects grid → tap a card. The app copies the project payload into that studio's
   draft folder, then navigates to the studio with the project id; the editor loads it and keeps
   the title bound (the saved title replaces the generic studio title in the toolbar). A project
   whose payload is missing shows a localized "could not be opened" message instead of an empty
   editor. If an unfinished draft exists, a dialog asks before replacing it, with a Cancel option
   and an explicit "Open anyway".
4. **Rename / delete** — long-press a card. Rename accepts up to 80 characters and falls back to
   the studio's localized default when the field is blank. Delete removes the project and thumbnail
   only; exports stay where they are.

## 4. Crash recovery versus projects

There are two different persistence layers, and they must not be confused:

| Layer | When written | Cleared | Purpose |
|---|---|---|---|
| **Studio draft** | debounced while editing | explicit exit (and only when the project flush succeeded) | process-death recovery |
| **Project** | explicit Save, and on leave for a bound project | only when the user deletes it | reopening later |

- Process death → next entry into the studio restores the draft with the "recovered" notice; the
  user can save it as a new project.
- Explicit exit with a bound project → the newest edit is flushed to the project, then the draft is
  cleared. If the flush fails (disk full, unreadable payload), the draft is **kept** so the edit is
  not lost; the next studio entry offers recovery.
- Explicit exit without a project → the draft is cleared; the edit is deliberately discarded.

## 5. Video-specific rules

- A video project stores **URIs**, not copied video bytes. The picker grant is made persistable
  with `takePersistableUriPermission` when clips are added and when a project is rebuilt, so a
  saved project can reopen after process death or an app restart. If the user moves/deletes the
  underlying file, the clip is skipped and a localized "some clips could not be read" notice is
  shown; the rest of the timeline still opens.
- A project is capped at **3 minutes total** (and five clips). `VideoProjectCodec` clamps on load
  and `addClips` trims/drops beyond the cap, with a visible notice — never a silent truncation.
- Video draft/project payloads are versioned JSON; unknown enum values fall back to a safe default
  rather than failing the whole project.

## 6. Limits and non-goals

- No cross-device transfer, no cloud sync, no import/export of a project file, no project sharing.
- No automatic cleanup/ageing: projects stay until the user deletes them or uninstalls the app.
- Thumbnails are JPEG on white; transparency in a design thumbnail is flattened, which is expected.
- A project is bound to its studio kind. Changing a project's kind is not supported.
- Project storage counts against the app's private storage; the Settings storage row reports
  app storage, not a per-project breakdown.

## 7. Verification status (0.2.2)

Overall unit suite: **90 tests / 18 suites / 0 failures**. Instrumented: **5/5 tests green on each of
two API 36 emulators (10/10 total)**. UI smoke on the API 36 emulator covered the rows below; nothing was
run on physical hardware.

| Check | Status |
|---|---|
| Unit tests: `ProjectRepositoryTest`, `VideoProjectCodecTest`, draft-store tests | **automated, passing** (part of the 90) |
| Instrumented tests: manifest permission, export, photo draft, model storage | **5/5 green on `review36b` and `sighaqa36`** (2026-11-22 00:14) |
| Create → save → back → reopen | **emulator pass** for Photo, Design, AI cut-out and Video (Design on the signed release APK; Photo/Video on a debug build of the same tree) |
| Rename, delete | **emulator pass** (Design project renamed and deleted; empty state confirmed) |
| Missing/corrupt payload, disk-full save | **logic unit-tested** (missing directory dropped by `refresh()`, typed save failures); no UI smoke |
| Process death with an open project | **emulator pass for draft retention**: force-stop while a bound Photo project was open, relaunch, opening the project showed the "unfinished edit" warning and "Open anyway" loaded the project; flush-failure logic unit-tested |
| AI cut-out project save/reopen | **emulator pass on the signed release**: portrait cut-out saved as "Cut-out project" → force-stop → relaunch → Projects (card thumbnail shows the cut-out) → open (draft warning "Open anyway") → toolbar shows **Cut-out project** and the cut-out image with transparent checkerboard renders. This run caught and verified the fix for a draft-restore race that left the toolbar generic |
| Real device | **not run** — all project checks so far are emulator-only |

Projects have not been verified on physical hardware; treat real-device behaviour as unverified.

## 8. Files

- `core/project/ProjectRepository.kt` — index, atomic writes, payload copy, thumbnail.
- `projects/ProjectsScreen.kt` — Projects/Exports tabs, create/open/rename/delete.
- `projects/ProjectSaveButton.kt` — shared toolbar/rail save action and title dialog.
- `core/draft/PhotoDraftStore.kt`, `DesignDraftStore.kt`, `AiDraftStore.kt`,
  `VideoDraftStore.kt` — the payload formats owned by each studio.
- `core/draft/VideoProject.kt` — versioned video payload and 3-minute clamp.
- `navigation/Routes.kt`, `navigation/MidadNavHost.kt` — `projectId` argument per studio route.
