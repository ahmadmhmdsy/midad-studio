# Midad Studio — store and press assets

Public assets for Midad Studio (استوديو مِداد): app icon, feature graphic, and a set of Arabic
phone screenshots of the app. The files live in this folder and are served with the rest of the
repository, so they can be used by anyone writing about or linking to the project.

> **Screenshot provenance note.** The phone screenshots in `screenshots/` were captured from an
> earlier pre-release build (version 0.2.0, versionCode 2), not from the current 0.2.3 build. The
> current UI is materially the same, but small details — some labels, dialog surfaces, numeral
> formatting and export-size names — may differ in v0.2.3. A separate, current set of seven English
> captures of the signed v0.2.3 build lives in [`screenshots-v023/`](screenshots-v023/README.md), with
> its own provenance and hashes; use that set when the current build must be shown. The icon and
> feature graphic are generated from the app's own launcher artwork and palette rather than from a
> separate design pass.

## What is in this folder

```
icon-512.png                    512 x 512 px, 32-bit RGBA (opaque), app icon
feature-graphic-1024x500.png    1024 x 500 px, 24-bit RGB, no alpha, feature graphic
screenshots/01-home.png ... 08-privacy.png
                                8 phone screenshots, 1080 x 1920 px (9:16), 24-bit RGB
screenshots/extras/             further captures that are not part of the eight-frame set
```

### Files, sizes and checksums

```
File                                          Bytes      SHA-256
icon-512.png                                  5,877      82CA0C61AFEE75A3F820B9AEB3DD2E14903806AD0D76F97C5A8FB3D3BEE11F6C
feature-graphic-1024x500.png                 26,948      70B4AF12097F57E53F6169FD1C5A03F9E2494EEDBFEDB9B756C21361B72308B9
screenshots/01-home.png                     223,535      7F7E5A78B514C61FAA207FC8753FA5E824D5D71722A9093EA527F3153B59CAEA
screenshots/02-photo-editor-adjust.png      259,502      992CD6D6472ECE1D987C6026C10813C41903506A1165AD47D1FAB8EA50087145
screenshots/03-ai-lab.png                   120,164      9CEBF1B80F0B7F83E32CBB49C5B976F475F5C355C0954CD94679BA092615A77B
screenshots/04-design-editor.png             72,332      75BC1DC944A1F9464DAFBB858F93CC66A7C5E9EF09826BE83CA29C48B6B2BB90
screenshots/05-collage.png                  304,578      0BD31C4E8146DED65CF0DA12DC6042494B6ECDB2341375131E77C554A2342932
screenshots/06-template-gallery.png         111,490      80E4F3D5DB3E5F0A0B4D89703137AEFC1C6FF441E053619DA12A141AFAC9037E
screenshots/07-export-sheet.png             121,996      FA457E1DF66B5DDA3F8952CB8DEA9CF2E38572A5941088EC7951E39E49ED3803
screenshots/08-privacy.png                  109,563      18C18D68FA239ED2575CDD5C4DA1809559466D9D3760A98A17C86179F7F3B689
screenshots/extras/design-start.png          82,947      36044F7DE46EE38A4EF588B81AD1F86711A5088F46D0017935AF0CEC196E6E0F
screenshots/extras/settings.png              92,453      5F872897E3E312F9D5D6764BD4E11464063A55153E1F7ECFF917A9B1C0131C34
screenshots/extras/video-start.png           55,710      1AD21FFFDCB9D7AF908474786F7930F5F34ADF9BCD3142FF955996C60F6445B7
screenshots/extras/design-layers-sheet.png   74,187      D7C32CECB0E630CC2B90A69963E22CA81BE451223B53243AE8F62803EA0EBD88
screenshots/extras/exports-grid.png         366,835      AC3418F50B83129F71E584FAB169D405B647E2C9ABF49A13861A9C3A15AC65A6
```

Verify any file with PowerShell:

```powershell
(Get-FileHash .\icon-512.png -Algorithm SHA256).Hash
```

## Image specifications

| Asset | Dimensions | Format | Alpha | Bytes | Notes |
|---|---|---|---|---|---|
| App icon | 512 x 512 | 32-bit PNG | yes (fully opaque canvas, no rounded corners baked in) | 5,877 | Generated from the launcher vector and the ink colour |
| Feature graphic | 1024 x 500 | 24-bit PNG | no | 26,948 | Paper palette and wordmark |
| Phone screenshots | 1080 x 1920 (9:16) | 24-bit PNG | no | 72–305 KB each | No device frame, no burned-in marketing text |

The screenshot set also satisfies the common store requirements for phone images — PNG, 9:16,
long side within 320–3840 px, no transparency, under 8 MB per file — but it is provided here first
as project documentation, not as a claim of store availability. Midad Studio is not currently
distributed through Google Play.

## The eight frames

Captions below are suggested copy for pages that support captions. No text is burned into the
images.

| # | File | Screen | What the frame shows | Suggested caption (AR / EN) |
|---|---|---|---|---|
| 1 | `01-home.png` | Home | «مِداد» wordmark, the promise line, the four studio cards (Photo, Video, Design, AI Lab) and the latest-exports row | أربعة استوديوهات في تطبيق واحد / Four studios, one app |
| 2 | `02-photo-editor-adjust.png` | Photo studio | A landscape photo mid-edit with the adjust panel open — الضوء واللون group, exposure at **+0.2**, per-group and global reset, undo/redo | اضبط الضوء واللون والتفاصيل / Light, colour and detail |
| 3 | `03-ai-lab.png` | AI Lab | On-device cutout landing: «يعمل على هذا الجهاز», the two AI tools and the installed-model chips | أزل الخلفية على جهازك / Remove a background on-device |
| 4 | `04-design-editor.png` | Design editor | An opened Arabic template (event poster) with headline, date and place, plus the layer rail: طبقات · خلفية · شكل · صورة · نص | قوالب جاهزة بنص عربي / Templates with Arabic text |
| 5 | `05-collage.png` | Design → Collage | A four-image collage (sage · saffron · indigo · terracotta) with paper gutters and the grid picker | كولاج من صورك / Collage from your photos |
| 6 | `06-template-gallery.png` | Design → Templates | Four of the seven original layered templates, with names and canvas sizes | تصاميم أصلية من طبقات / Seven original templates |
| 7 | `07-export-sheet.png` | Photo studio → Export | The export sheet on the paper surface: تصدير الصورة, الصيغة JPG/PNG, جودة JPG, الضلع الأطول, إلغاء / تصدير | صدّر بلا علامة مائية / Export without a watermark |
| 8 | `08-privacy.png` | Settings → الخصوصية | The in-app privacy statement: offline, media stays on the device, models verified by checksum, no accounts/ads/analytics | خصوصية بالتصميم / Private by design |

The eight-frame set does not include a video-editing screen; `extras/video-start.png` shows the
Video studio entry screen. The v0.2.3 build adds two surfaces that are not captured here — the
exact full-project video preview and the shared export result card. A current (v0.2.3) English set
is published in [`screenshots-v023/`](screenshots-v023/README.md); this Arabic set is kept as a
reference for the Arabic-UI listing and still needs a re-shoot from a 0.2.3+ build before it is
submitted to any store.

### Extras (supporting captures, not part of the eight-frame set)

- `design-start.png` — the "start on paper" screen with the three canvas presets and the two entrances.
- `settings.png` — the full Settings screen (theme, AI models, storage, legal, privacy, version).
- `video-start.png` — the Video studio entry screen.
- `design-layers-sheet.png` — the layers sheet over a template: reorder / show-hide / delete controls.
- `exports-grid.png` — the Exports grid (illustrative sample exports).

## Capture setup

| Setting | Value |
|---|---|
| Device | AOSP emulator, API 36 (Android 16), x86_64 |
| Display | physical 1080 x 2400 at 420 dpi; captures taken at **1080 x 1920** (`adb shell wm size 1080x1920`, `wm density 420`), so the screenshots are true 1080 x 1920 PNGs without cropping |
| System navigation | **gesture navigation**, so no system navigation bar covers app content: `adb shell cmd overlay enable-exclusive --category com.android.internal.systemui.navbar.gestural` |
| App locale | `ar-IQ` via `cmd locale set-app-locales`; system locale stays `en-US`, so the system Photo Picker is LTR |
| Theme | system theme preference against a light system → light Paper theme |
| Font scale | 100 % |
| Frame | no device frame, no burned-in marketing text, no dialogs from other apps; status bar shows time and system icons only |
| Method | `adb exec-out screencap -p`, then converted to 24-bit PNG (alpha removed) |
| Demo media | Midad's own exports (the seven template renders), generated gradient stills (a 1200 x 1200 set and one 1600 x 1000 landscape still), and three 3-second gradient clips made locally. No third-party photo, stock image or person appears in any frame of this 0.2.0 set (the v0.2.3 set is documented separately) |

```powershell
$adb = 'C:\Android\Sdk\platform-tools\adb.exe'
& $adb -s emulator-5554 shell wm size 1080x1920
& $adb -s emulator-5554 shell wm density 420
& $adb -s emulator-5554 shell cmd overlay enable-exclusive --category com.android.internal.systemui.navbar.gestural
& $adb -s emulator-5554 shell cmd locale set-app-locales com.midad.studio --user 0 --locales ar-IQ
& $adb -s emulator-5554 shell am force-stop com.midad.studio
& $adb -s emulator-5554 shell am start -n com.midad.studio/.MainActivity
# drive the screen, then:
& $adb -s emulator-5554 exec-out screencap -p > frame.png
# afterwards:
& $adb -s emulator-5554 shell wm size reset
& $adb -s emulator-5554 shell wm density reset
& $adb -s emulator-5554 shell cmd overlay enable-exclusive --category com.android.internal.systemui.navbar.threebutton
```

Each capture is converted to a 24-bit PNG (alpha removed) so the screenshots contain no
transparency; the icon keeps its 32-bit RGBA canvas. A PNG is alpha-free when byte 25 of the file
(the colour type) is `2`.

## Licence and attribution

Screenshots and graphics in this folder depict Midad Studio and are © 2026 Ahmad Mahmoud. They may
be reused to describe Midad Studio with attribution. The app's code licence is stated in the
repository [LICENSE](../../LICENSE); third-party libraries and AI models keep their own licences
(see [docs/product/MODEL_LICENSES.md](../product/MODEL_LICENSES.md) and
[docs/product/licenses/](../product/licenses/)).

The demo media in `screenshots/` are the app's own exports and generated images; no third-party
photograph, artwork or person appears in that 0.2.0 set. The v0.2.3 set in `screenshots-v023/`
includes one CC0 public-domain test photograph, documented with its licence, source, crop and
hashes in [screenshots-v023/README.md](screenshots-v023/README.md).
