# Midad Studio 0.2.3 screenshots (public)

Captured on **2026-11-23** from the signed **Midad Studio 0.2.3** (versionCode 5) release build
installed on an **API 36 emulator** — 1080 × 2400 px, English locale, light theme, system photo
picker. Raw device captures: no mockup frames, no retouching, no compositing. The emulator used 3-button system
navigation, so the status bar and the navigation bar are part of every frame.

| File | What it shows | Bytes | SHA-256 |
|---|---|---|---|
| `01-home.png` | Home — the four studios, Projects and Recent exports | 364,572 | `91B9A0CCAE9CEC35D26346BC6E016C2ACB5F29B3D8B5D6B91350640347F06204` |
| `02-design-start.png` | Design — canvas presets, ways to start, template shelf | 145,822 | `0EFCD21A8BB31FCD4F426153D5E680B46FE1D5AE1FD29560A4DBC8C857700C2F` |
| `03-template-gallery.png` | Templates — the seven original layered layouts | 182,814 | `D843D3B1542CF6F37F177EECC7AE86D95B284AB139F650A237B1DB570C66CDCA` |
| `04-template-editor.png` | Canvas editor — a template open, layer tools, Export | 109,895 | `7C788AF98D3049906D85C1E810ABFD02E401D04BA382B994EC3824CC0D1C6BC7` |
| `05-photo-adjust.png` | Photo editor — light and colour controls | 779,354 | `37B15D47EBE8830ACAB5D9B77C661EC22BA86E900D395DEC073349F4D2220E49` |
| `06-ai-cutout.png` | Background removal — transparent cut-out, model and backdrop choices | 623,962 | `7E8C53A4DC5C3309749C5FB81D65BBB94B0E00DD346E82BD734D0BF6FC589352` |
| `07-ai-lab.png` | AI Lab — tools, six models, installed state | 235,073 | `6B2EB08576C437747EA2EA60D4062060A5AB0B62B450A43EA5341F5D25BC2D5E` |

Reproduce: install the release APK on an API 36 emulator, then
`adb exec-out screencap -p > 01-home.png` (or `adb shell screencap -p /data/local/tmp/x.png`
and pull it). The captures include the emulator status and navigation bars.

## Honest notes

- These are emulator captures, like the rest of the acceptance evidence in
  [docs/qa/BUILD_REPORT.md](../../qa/BUILD_REPORT.md). No real-device QA has been completed.
- The portrait used to demonstrate import, editing and background removal is a **CC0 1.0
  public-domain test photograph**, cropped from its 1920 × 1280 original to a 1280 × 1280 square.
  Source: Wikimedia Commons file
  [Gray-haired man portrait (Unsplash).jpg](https://commons.wikimedia.org/wiki/File%3AGray-haired_man_portrait_%28Unsplash%29.jpg)
  — an Unsplash upload (Foto Sushi, 2016) imported under CC0; no attribution is required, and the
  person shown is not affiliated with or endorsing Midad Studio. It is not bundled with the app, is
  not covered by the project licence, and is not redistributed as an asset of Midad Studio; it
  appears only inside these illustrative screenshots. The two frames were re-captured on
  2026-11-23 with this photograph, replacing an earlier test portrait of unverified origin.
- The earlier Arabic set in [../screenshots/](../screenshots/) was captured from the 0.2.0
  pre-release build and is documented there with its own provenance and hashes.
