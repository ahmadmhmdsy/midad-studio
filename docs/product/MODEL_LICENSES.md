# AI model licence matrix and technical manifest — Midad Studio

**Prepared:** 2026-11-16 · **Updated:** 2026-11-22 for release 0.2.3 (versionCode 5)
**Scope:** on-device AI models for background removal / subject cutout, background blur / replace, and optional upscale/enhance in an offline-first Android photo/video editor. **Hard requirement: commercial use allowed, models bundled or downloadable at runtime.**
**Companion file:** [`model_manifest.json`](./model_manifest.json) — machine-readable manifest of the models integrated in the app.
**Licence texts:** [`licenses/`](./licenses/) — verbatim Apache-2.0 texts per model id, model-card NOTICE, and per-model evidence records (DeepLab-V3, MODNet).

> **Release note — 0.2.3 (2026-11-22).** The shipped inference runtime is **LiteRT 2.3.2**
> (`com.google.ai.edge.litert:litert`, Apache-2.0), the maintained TensorFlow Lite runtime;
> `org.tensorflow:tensorflow-lite` is not used — its last release, 2.17.0, only relocates to LiteRT.
> The catalogue grows from four to **six** models: MediaPipe **DeepLab-V3** (2,780,176 B) and
> **MODNet portrait matting** (26,166,776 B) are added as **runtime downloads** — not bundled, so they
> are not in the APK/AAB. The app has no TensorFlow Lite and no MediaPipe Android dependency. Evidence
> for the two additions:
> [`licenses/modnet-LICENSE-EVIDENCE.md`](./licenses/modnet-LICENSE-EVIDENCE.md) and
> [`licenses/mediapipe-deeplab-v3-LICENSE-EVIDENCE.md`](./licenses/mediapipe-deeplab-v3-LICENSE-EVIDENCE.md).

## How this was verified (evidence standard)

Everything below was checked against **official sources only** (model-card PDFs on Google storage, GitHub repo LICENSE files via the GitHub licensing API, Hugging Face model-card API, Maven POM/metadata on Google Maven and Maven Central, and the MediaPipe source tree). Blog posts and third-party mirrors were not used.

All five MediaPipe `.tflite` binaries were **downloaded on 2026-11-16** and SHA-256 hashed locally, and their tensor specs were read **from the binaries themselves** (TFLite flatbuffer: input/output tensor shape+dtype, embedded TFLite Metadata `NormalizationOptions` mean/std, and the `SEGMENTER_METADATA` custom metadata activation field). Pre/post-processing semantics were cross-checked against MediaPipe source files (citations in §3.2). Nothing is guessed where an official source exists; every unresolved item is labelled **UNRESOLVED**. The MODNet-LiteRT conversion was downloaded and hashed on 2026-11-22 (§2.2), and its tensor spec was read from its flatbuffer the same day.

> **Licensing nuance that matters (updated 2026-11-22):** the licence field embedded in the MediaPipe `.tflite` metadata is **empty** in all five MediaPipe models (verified by parsing the flatbuffers). The primary evidence is the **model card PDF**, which explicitly says *"Licensed under Apache License, Version 2.0"* for the Selfie, Multiclass and Hair models, plus the model-level licence record for DeepLab-V3 (Kaggle Models API, §2.1 — later also stated on the official LiteRT mirror card, §2.2 of the evidence file). The v1 text treated the missing MediaPipe card as a licence obstacle for DeepLab-V3; that was wrong — the licence record was established on the publisher's API record on 2026-11-16, and the model ships as a runtime download in 0.2.3. The missing card remains a documented caveat, not an obstacle (`licenses/mediapipe-deeplab-v3-LICENSE-EVIDENCE.md` §3).

---

## 0. TL;DR verdict

| Verdict | Models | Bottom line |
|---|---|---|
| **SHIP-BUNDLED (2)** | MediaPipe **SelfieSegmenter (square, 256x256, float16)** — 249,537 B; **SelfieSegmenter (landscape, 256x144, float16)** — 250,177 B | Apache-2.0, stated on the official model card. Safe for commercial redistribution inside the APK/AAB. These are the default segmenters. |
| **DOWNLOAD-ONLY (4)** | MediaPipe **SelfieMulticlass 256x256 (float32)** — 16,371,837 B; MediaPipe **HairSegmenter 512x512 (float32)** — 781,618 B; MediaPipe **DeepLab-V3 257x257 (float32)** — 2,780,176 B; **MODNet-LiteRT portrait matting 512x512 (float32)** — 26,166,776 B | All **Apache-2.0** with recorded evidence (model cards; Kaggle Models API + LiteRT mirror card; upstream repository LICENSE + conversion card). Downloaded at runtime, SHA-256 verified, stored app-private; NOTICE + in-app licence screen required. |
| **NOT ADOPTED (licence fine, no 0.2.x product need)** | U2-Net (weights/training-data provenance unresolved), BiRefNet (MIT), Real-ESRGAN (BSD-3), SAM/SAM 2 (Apache-2.0) | Not excluded for licence reasons alone — conversion/size/UX cost. See §1 and §6. |
| **EXCLUDE — non-commercial / contaminated** | RMBG-1.4 and RMBG-2.0 (**non-commercial**); CodeFormer (**NTU S-Lab License 1.0, non-commercial**); GFPGAN (**third-party NVIDIA StyleGAN2 / CC BY-NC-SA components**); AnimeGANv2 (**non-commercial**) | Do not ship. Reasons per row in §1; verified artifacts in §2. |
| **RUNTIMES** | `com.google.ai.edge.litert:litert` **2.3.2** (Apache-2.0, the maintained TFLite runtime; `org.tensorflow:tensorflow-lite` 2.17.0 is the last release on the old coordinate and its POM relocates to LiteRT). `com.google.mediapipe:tasks-vision` **1.2.3** (Apache-2.0) is the vendor high-level alternative but is **not used**: the AI studio runs TensorFlow Lite / LiteRT only, with no MediaPipe dependency. `com.microsoft.onnxruntime:onnxruntime-android` **1.32.0** (MIT) only if PyTorch-lineage models are adopted later. | For the TFLite-only path: LiteRT + the exact preprocessing/postprocessing spec in §3. No MediaPipe AAR needed. **LiteRT's AAR-root `LICENSE` + `THIRD_PARTY_NOTICE.txt` are not packaged by AGP, so the 18:34Z build ships both verbatim as app assets (`assets/models/LiteRT-*.txt`) and exposes them from the licences screen (see §5 item 2).** |

**Recommended set for 0.2.3 (as shipped):** bundle the two selfie models (square = default person/background cutout; landscape for a fixed landscape video canvas), and offer four downloads — multiclass (16.4 MB, 6-class hair-aware "HD" cutout), hair (782 KB), DeepLab-V3 (2.78 MB, 21-class general objects/scenery) and MODNet (26.17 MB, soft portrait matte). All six are in `app/src/main/assets/models/manifest.json`; exact hashes in §2.

**Verification standard:** every model in the catalogue has a recorded licence name, evidence URL, fetch date and SHA-256, kept in [`licenses/`](./licenses/) and in [`model_manifest.json`](./model_manifest.json). This document supplies that evidence; it is not a legal opinion. **0.2.3 status (2026-11-22):** all six catalogue models are Apache-2.0 with recorded evidence; DeepLab-V3 (§2.1) and MODNet (§2.2) are the records added for this release.

---

## 0.1 What release 0.2.3 actually ships — licence consolidation

This table is the release-time answer for "what licences are inside the APK/AAB", as distinct from the
per-model licence work below. Versions are the ones declared directly in `app/build.gradle.kts` — this
repository does not use a Gradle version catalog (0.2.3 = versionCode 5, versionName "0.2.3").

| Component | Version in 0.2.3 | Licence | In the artifact? | Notes |
|---|---|---|---|---|
| **LiteRT** (`com.google.ai.edge.litert:litert`) | **2.3.2** | **Apache-2.0** | Yes — `libLiteRt.so`, `liblitert_jni.so` per ABI | The maintained TFLite runtime; replaces the TensorFlow Lite 2.16.1 dependency of the 0.1.0 draft. Its AAR-root `LICENSE` + `THIRD_PARTY_NOTICE.txt` are **not** packaged by AGP |
| TensorFlow Lite (`org.tensorflow:tensorflow-lite`) | 2.17.0 is the last release; **not used** | Apache-2.0 | **No** in the 2026-11-22 build | Its POM relocates to LiteRT. No `libtensorflowlite_jni.so` exists in the release APK, the AAB, or the 0.2.2 APK on disk (checked 2026-11-22) |
| **Media3** (ExoPlayer, Transformer, Effect, Common) | **1.5.1** (pinned) | **Apache-2.0** | Yes | No separate commercial licence needed; NOTICE obligations only |
| **Kotlin** stdlib + `kotlinx` coroutines | **2.0.21** | **Apache-2.0** | Yes | — |
| **Jetpack Compose** (UI, Material 3, foundation, runtime) | via Compose BOM (declared in `app/build.gradle.kts`) | **Apache-2.0** | Yes | Exact BOM version is recorded in `app/build.gradle.kts`, not here |
| **AndroidX** (activity, lifecycle, navigation, core, graphics, exif, etc.) | declared in `app/build.gradle.kts` | **Apache-2.0** | Yes | Includes `androidx.graphics.path` native lib |
| **Bundled segmentation models (2)** — MediaPipe SelfieSegmenter square (249,537 B) and landscape (250,177 B) | as catalogued in §0 | **Apache-2.0** (model card) | Yes, in `assets/models/` | Apache-2.0 text (`LICENSE-Apache-2.0.txt`) + `NOTICE.txt` ship in the app |
| **Download-only models (4)** — SelfieMulticlass 16,371,837 B; HairSegmenter 781,618 B; **DeepLab-V3 2,780,176 B** (Google storage); **MODNet 26,166,776 B** (Hugging Face) | as catalogued in §0 | **Apache-2.0** (model cards; Kaggle API + mirror card for DeepLab-V3; upstream LICENSE + conversion card for MODNet) | **No** — fetched only on user request; the app verifies the pinned SHA-256 | Our own distribution when we serve it: Apache-2.0 text + attribution + NOTICE + licence screen |
| **A model the user installs from elsewhere** | publisher-defined | **The publisher's licence, not ours** | **No** | Not bundled, not in our catalogue, not relicensed by us; the app must not imply we license or endorse it |

**Bottom line for store metadata and the privacy policy:** every byte the app itself ships is Apache-2.0
(LiteRT, Media3, Kotlin, Compose, AndroidX, the two bundled models); the four download-only models are Apache-2.0
as well; and a model obtained from a third party is licensed by its publisher — the app only runs the file the user
supplied.

**Attribution surface (re-verified 2026-11-22T18:34Z):** the in-app licences screen (full-text viewer), `assets/models/NOTICE.txt` (6,512 B,
sha256 `2fe0d5f0…`; covers all six models and LiteRT 2.3.2), the MediaPipe Apache-2.0 copy and the LiteRT `LICENSE` + `THIRD_PARTY_NOTICE.txt` in `assets/models/`,
`docs/product/licenses/`. Media3, Kotlin, Compose and AndroidX entries must be aggregated into the same screen
(AGP can generate them). **Note:** the LiteRT 2.3.2 AAR ships `LICENSE` (13,575 B, sha256 `71c6915d…`) and
`THIRD_PARTY_NOTICE.txt` (1,960,672 B, sha256 `db6e1ff4…`) at the **AAR root** — not `META-INF/NOTICE` — and AGP
does **not** copy AAR root files into the APK. **In the 2026-11-22T18:34Z rebuild:** both files are app
assets (`assets/models/LiteRT-LICENSE.txt`, `assets/models/LiteRT-THIRD_PARTY_NOTICE.txt`, byte-identical to the
AAR root, verified) and are embedded in the rebuilt APK and AAB; the licences screen opens both (§5 item 2).

**Release-artifact note (2026-11-22T18:34Z rebuild):** the final 0.2.3 APK is 22,072,532 B (sha256 `aa7674d5…`)
and the AAB 15,480,566 B (sha256 `5bf7b419…`); both embed `assets/models/NOTICE.txt` = 6,512 B / `2fe0d5f0…`,
`LiteRT-LICENSE.txt` = 13,575 B / `71c6915d…`, `LiteRT-THIRD_PARTY_NOTICE.txt` = 1,960,672 B / `db6e1ff4…` and
`manifest.json` = 23,925 B / `9fde9460…` (verified by extracting the zip entries). The superseded 17:36Z pair (21,829,868 B `5e7ced5d…` / 15,240,413 B `082328af…`, old 3,074 B NOTICE) was
replaced on 2026-11-22T21:47+03:00: the root-level convenience copies `Midad-Studio-0.2.3-release.apk/.aab`
and the copies one level up are now byte-identical to these 18:34Z outputs (hashes above, verified).

---


## 1. Licence matrix — every model evaluated

Legend: **SHIP-BUNDLED** = binary may be packaged in the app; **DOWNLOAD-ONLY** = licensed for commercial use, but we chose to fetch it at runtime for size/quality reasons (not a legal restriction); **NOT ADOPTED** = licence acceptable but no 0.2.x product need (conversion/size/UX cost); **EXCLUDE** = do not use (non-commercial or licence-contaminated).

Every licence evidence URL below was fetched on **2026-11-16**; the 0.2.3 additions (DeepLab-V3 mirror evidence, MODNet) were fetched on **2026-11-22**. "Artifact size + SHA-256" is the exact binary we ship or bundle, hashed locally; "n/a" means no binary was adopted for shipping (licence verdict only).

| Model (weights artifact) | Code licence | Weights licence | Licence evidence (URL, fetched 2026-11-16) | Commercial use | Redistribution / bundling | Attribution / NOTICE | Artifact size + SHA-256 | Verdict |
|---|---|---|---|---|---|---|---|---|
| **MediaPipe SelfieSegmenter** square + landscape (.tflite) | Apache-2.0 (MediaPipe repo LICENSE) | Apache-2.0 — model card: "Licensed under Apache License, Version 2.0" | [Model card PDF](https://storage.googleapis.com/mediapipe-assets/Model%20Card%20MediaPipe%20Selfie%20Segmentation.pdf) · [docs](https://ai.google.dev/edge/mediapipe/solutions/vision/image_segmenter) · [MediaPipe LICENSE](https://github.com/google-ai-edge/mediapipe/blob/master/LICENSE) | **Yes** | Allowed; ship Apache-2.0 LICENSE + NOTICE | Tingbo Hou, Siargey Pisarchyk, Karthik Raveendran (Google), model card May 6, 2021 | 249,537 B, sha256 191ac9529ae506ee0beefa6b2c945a172dab9d07d1e802a290a4e4038226658b (square f16 v1); 250,177 B, sha256 490e9ea734313e0de10fa0cd9e3c6133e36ea4db2b7a49bde9ef019f72796b8e (landscape f16 v1) | **SHIP-BUNDLED** |
| **MediaPipe SelfieMulticlass 256x256** (.tflite) | Apache-2.0 (MediaPipe repo LICENSE) | Apache-2.0 — model card: "Licensed under Apache License, Version 2.0" | [Model card PDF](https://storage.googleapis.com/mediapipe-assets/Model%20Card%20Multiclass%20Segmentation.pdf) · [docs](https://ai.google.dev/edge/mediapipe/solutions/vision/image_segmenter) | **Yes** | Allowed (our runtime download is our own distribution); same NOTICE obligations | Adel Ahmadyan (Google), model date May 10, 2023 | 16,371,837 B, sha256 c6748b1253a99067ef71f7e26ca71096cd449baefa8f101900ea23016507e0e0 (f32 v1) | **DOWNLOAD-ONLY** |
| **MediaPipe HairSegmenter 512x512** (.tflite) | Apache-2.0 (MediaPipe repo LICENSE) | Apache-2.0 — model card: "Licensed under Apache License, Version 2.0" | [Model card PDF](https://storage.googleapis.com/mediapipe-assets/Model%20Card%20-%20Hair%20Segmentation.pdf) · [docs](https://ai.google.dev/edge/mediapipe/solutions/vision/image_segmenter) | **Yes** | Allowed; same NOTICE + cite Tkachenka et al. | Google researchers, 2019, version 2019-01-14; cite A. Tkachenka et al., CVPR Workshop 2019 | 781,618 B, sha256 2628cf3ce5f695f604cbea2841e00befcaa3624bf80caf3664bef2656d59bf84 (f32 v1) | **DOWNLOAD-ONLY** |
| **MediaPipe DeepLab-V3** 257x257 (.tflite) | Apache-2.0 (MediaPipe repo LICENSE) | Apache-2.0 — Kaggle Models API licenseName "Apache 2.0" for tensorflow/deeplabv3 TF Lite instances (default v1 2,779,264 B; metadata v2 2,780,051 B), **and** the official LiteRT mirror card states `license: apache-2.0` (repo commit `739202fd…`, lastModified 2026-11-19; file LFS sha256 = local hash). No MediaPipe model card exists. | [Kaggle Models API JSON](https://www.kaggle.com/api/v1/models/list?pageSize=20&search=deeplab) · [model page](https://www.kaggle.com/models/tensorflow/deeplabv3) · [LiteRT mirror card](https://huggingface.co/litert-community/MediaPipe-ImageSegmenter-DeepLab-V3) · [evidence file](./licenses/mediapipe-deeplab-v3-LICENSE-EVIDENCE.md) | **Yes** | Allowed under Apache-2.0; the MediaPipe build is a modified distribution (not a byte-match to Kaggle) — permitted. Ships as a **runtime download** in 0.2.3 (not bundled), with NOTICE §5 + licence-screen entry. | TensorFlow (Kaggle org), published 2020-10-06; MediaPipe packaging by Google; mirror by `litert-community` | 2,780,176 B, sha256 ff36e24d40547fe9e645e2f4e8745d1876d6e38b332d39a82f0bf0f5d1d561b3 (f32 v1) — re-verified 2026-11-22 | **DOWNLOAD-ONLY (shipped in 0.2.3)** |
| **U2-Net / U2-Netp** | Apache-2.0 (repo LICENSE; GitHub spdx Apache-2.0) | No separate weights licence; author PyTorch checkpoints trained on **DUTS-TR, whose terms are UNRESOLVED** | [repo LICENSE](https://github.com/xuebinqin/U-2-Net) · official DUTS page (no licence text retrievable) | Probably, but **UNRESOLVED** | Not verified (weights + training data) | Repo copyright notices | n/a (no artifact adopted; conversion required) | **NOT ADOPTED** |
| **MODNet** (LiteRT conversion `modnet.tflite`) | Apache-2.0 (upstream repo LICENSE; GitHub API `spdx_id: Apache-2.0`) | Apache-2.0 — upstream LICENSE 11,357 B, blob `261eeb9e…`, raw sha256 `c71d239d…`; **and** the HF conversion card's front-matter `license: apache-2.0`, weights attributed to ZHKKKe/MODNet Apache-2.0 | [upstream repo](https://github.com/ZHKKKe/MODNet) · [upstream LICENSE](https://raw.githubusercontent.com/ZHKKKe/MODNet/master/LICENSE) · [HF conversion card](https://huggingface.co/litert-community/MODNet-LiteRT) · [evidence file](./licenses/modnet-LICENSE-EVIDENCE.md) | **Yes** | OK; the HF repo publishes the LiteRT conversion (26,166,776 B) — the app downloads it, unmodified, at runtime; no upstream NOTICE file | Zhanghan Ke, Jiayu Sun, Kaican Li, Qiong Yan, Rynson W.H. Lau (AAAI 2022); conversion by `litert-community` with two GPU re-authoring patches documented on the card | 26,166,776 B, sha256 f7d1e9eca9562abddb60e4b0a3ed0c29441ef09cd621954978f1613914cc64bc (LiteRT conversion; LFS oid matches) | **DOWNLOAD-ONLY (shipped in 0.2.3)** |
| **BiRefNet** | MIT (repo LICENSE; GitHub spdx MIT) | MIT — HF model card metadata license: mit | [repo LICENSE](https://github.com/ZhengPeng7/BiRefNet) · [HF card](https://huggingface.co/ZhengPeng7/BiRefNet) | **Yes** | OK, but PyTorch weights need conversion + size work | MIT notice, Copyright (c) 2024 ZhengPeng | n/a (no artifact adopted) | **NOT ADOPTED (post-0.2.3)** |
| **RMBG-1.4** | — (weights-only model) | bria-rmbg-1.4 custom licence — HF card: "available as a source-available model for non-commercial use ... To purchase a commercial license ..." | [HF card](https://huggingface.co/briaai/RMBG-1.4) | **No** (non-commercial) | Not allowed without a BRIA agreement | n/a | n/a | **EXCLUDE** |
| **RMBG-2.0** | — (weights-only model) | bria-rmbg-2.0, licence link CC BY-NC 4.0; gated model | [HF API cardData](https://huggingface.co/briaai/RMBG-2.0) | **No** (non-commercial) | Not allowed without a BRIA agreement | n/a | n/a | **EXCLUDE** |
| **Real-ESRGAN** (upscale/enhance) | BSD-3-Clause (repo LICENSE; GitHub spdx) | BSD-3-Clause project licence; author-published weights, no separate EULA found | [repo LICENSE](https://github.com/xinntao/Real-ESRGAN) | **Yes** per repo licence (training-data terms not re-verified) | OK; needs ONNX/ncnn conversion for Android | BSD-3 copyright notice | n/a (no artifact adopted) | **NOT ADOPTED (post-0.2.3)** |
| **GFPGAN** (face enhance) | Apache-2.0 for own code, but LICENSE explicitly excludes third-party components | **UNRESOLVED / high risk** — NVIDIA StyleGAN2 (§3.3 non-commercial only) + DFDNet (CC BY-NC-SA 4.0); face-restoration weights derive from them | [repo LICENSE + README §License](https://github.com/TencentARC/GFPGAN) | **No, not safely** | Do not redistribute | — | n/a | **EXCLUDE** |
| **CodeFormer** (face enhance) | NTU S-Lab License 1.0 — "Redistribution and use for non-commercial purpose in source and binary forms ..." | Non-commercial | [repo LICENSE](https://github.com/sczhou/CodeFormer) | **No** | Not allowed | — | n/a | **EXCLUDE** |
| **AnimeGANv2** (style transfer) | Official repo has NO LICENSE file (GitHub API 404); README says "freely available ... for non-commercial purposes"; the bryandlee port is MIT but its weights derive from the non-commercial upstream | Non-commercial (upstream) | [repo README §License](https://github.com/TachibanaYoshino/AnimeGANv2) · [MIT port LICENSE](https://github.com/bryandlee/animegan2-pytorch) | **No** | Not allowed | — | n/a | **EXCLUDE** |
| **SAM (Segment Anything)** | Apache-2.0 | Apache-2.0 — README: "The model is licensed under the Apache 2.0 license." | [repo README §License + LICENSE](https://github.com/facebookresearch/segment-anything) | **Yes** | OK | Apache NOTICE + repo | n/a (ViT-B ≈ 375 MB; not adopted) | **NOT ADOPTED** |
| **SAM 2** | Apache-2.0 | Apache-2.0 — README: "The SAM 2 model checkpoints ... are licensed under Apache 2.0" | [repo README §License](https://github.com/facebookresearch/sam2) | **Yes** | OK | Apache NOTICE + repo | n/a (not adopted) | **NOT ADOPTED** |

### Datasets checked (for weights provenance)
- **DIS5K / DIS** (BiRefNet etc.): repo LICENSE = **Apache-2.0** (verified via GitHub API).
- **P3M-10k** (portrait matting): repo LICENSE = **MIT** (verified via GitHub API).
- **DUTS** (U2-Net): no licence text retrievable from the official dataset page → **UNRESOLVED** (part of why U2-Net is excluded).

---

## 2. Verified MediaPipe model artifacts (download + SHA-256)

The five MediaPipe files were downloaded **directly from Google's official bucket** and hashed on 2026-11-16 (DeepLab-V3 re-downloaded and matched 2026-11-22). The MODNet LiteRT conversion is a Hugging Face artifact, downloaded and hashed on 2026-11-22 (§2.2).

| Artifact | Version | Bytes | SHA-256 | Artifact Last-Modified (GCS) | Model card / licence |
|---|---|---|---|---|---|
| [`selfie_segmenter.tflite`](https://storage.googleapis.com/mediapipe-models/image_segmenter/selfie_segmenter/float16/1/selfie_segmenter.tflite) (square) | 1 | 249,537 | `191ac9529ae506ee0beefa6b2c945a172dab9d07d1e802a290a4e4038226658b` | 2023-05-07 05:14:12 GMT | May 6, 2021 — Apache-2.0 |
| [`selfie_segmenter_landscape.tflite`](https://storage.googleapis.com/mediapipe-models/image_segmenter/selfie_segmenter_landscape/float16/1/selfie_segmenter_landscape.tflite) | 1 | 250,177 | `490e9ea734313e0de10fa0cd9e3c6133e36ea4db2b7a49bde9ef019f72796b8e` | 2023-05-07 05:14:12 GMT | same card — Apache-2.0 |
| [`selfie_multiclass_256x256.tflite`](https://storage.googleapis.com/mediapipe-models/image_segmenter/selfie_multiclass_256x256/float32/1/selfie_multiclass_256x256.tflite) | 1 | 16,371,837 | `c6748b1253a99067ef71f7e26ca71096cd449baefa8f101900ea23016507e0e0` | 2023-04-26 03:33:35 GMT | May 10, 2023 — Apache-2.0 |
| [`hair_segmenter.tflite`](https://storage.googleapis.com/mediapipe-models/image_segmenter/hair_segmenter/float32/1/hair_segmenter.tflite) | 1 | 781,618 | `2628cf3ce5f695f604cbea2841e00befcaa3624bf80caf3664bef2656d59bf84` | 2023-04-26 03:33:37 GMT | 2019-01-14 — Apache-2.0 |
| [`deeplab_v3.tflite`](https://storage.googleapis.com/mediapipe-models/image_segmenter/deeplab_v3/float32/1/deeplab_v3.tflite) — **runtime download in 0.2.3** | 1 | 2,780,176 | `ff36e24d40547fe9e645e2f4e8745d1876d6e38b332d39a82f0bf0f5d1d561b3` | 2023-04-26 03:33:40 GMT | **no MediaPipe model card; Apache-2.0 recorded via Kaggle Models API (§2.1) and the official LiteRT mirror card (evidence file §6)** |
| [`modnet.tflite`](https://huggingface.co/litert-community/MODNet-LiteRT/resolve/main/modnet.tflite) — **runtime download in 0.2.3** | HF `main` | 26,166,776 | `f7d1e9eca9562abddb60e4b0a3ed0c29441ef09cd621954978f1613914cc64bc` | HF repo lastModified 2026-09-08 08:52:54Z | **Apache-2.0** — upstream repo LICENSE + HF conversion card (`licenses/modnet-LICENSE-EVIDENCE.md`); no NOTICE file upstream |

Embedded metadata in every MediaPipe file: `author = MediaPipe`, `version = 1`, `license = (empty)`, `min_parser_version = 1.5.0` (the MODNet conversion carries no such MediaPipe metadata).

Reproduce with PowerShell:

```powershell
$u = 'https://storage.googleapis.com/mediapipe-models/image_segmenter/selfie_segmenter/float16/1/selfie_segmenter.tflite'
curl.exe -sS -L -o selfie_segmenter.tflite $u
(Get-FileHash -Algorithm SHA256 .\selfie_segmenter.tflite).Hash.ToLower()
# expect 191ac9529ae506ee0beefa6b2c945a172dab9d07d1e802a290a4e4038226658b
```

### 2.1 DeepLab-V3 model-level licence evidence (recorded 2026-11-16)

The DeepLab-V3 file that the MediaPipe models table links (§2) has no MediaPipe model card, so a model-level licence record was assembled from the publisher's own API. Evidence recorded — full reproduction and negative evidence in [mediapipe-deeplab-v3-LICENSE-EVIDENCE.md](./licenses/mediapipe-deeplab-v3-LICENSE-EVIDENCE.md):

- **Licence: Apache-2.0.** Kaggle Models public API (GET https://www.kaggle.com/api/v1/models/list?pageSize=20&search=deeplab, HTTP 200, 152,613 bytes, fetched 2026-11-16) returns licenseName "Apache 2.0" for **both** TF Lite instances of tensorflow/deeplabv3: default v1 (2,779,264 B uncompressed — the deployment the MediaPipe file derives from) and metadata v2 (2,780,051 B). Model page: https://www.kaggle.com/models/tensorflow/deeplabv3 (author TensorFlow, published 2020-10-06, updated 2023-02-15).
- **No MediaPipe model card exists.** Full enumeration of the public mediapipe-assets bucket (5,956 keys) found 13 model-card PDFs — none for DeepLab-V3; the embedded TFLite metadata licence field is empty (same as every MediaPipe model). The missing card, not a restrictive licence, is what kept this flagged.
- **Status: verified (2026-11-16).** The record was independently re-checked by re-fetching the API and re-downloading the artifact (2,780,176 B, ff36e24d... — match): the Kaggle API field is the publisher's own machine-readable licence statement, and MediaPipe's docs identify the shipped file as the TF Lite deployment of that model. Caveats: (i) evidence is an API field, not a rendered licence panel or a MediaPipe model card (none exists); (ii) the MediaPipe build is not byte-identical to either Kaggle instance (125 B difference vs v2) — Apache-2.0 permits modified redistribution; (iii) if bundled it needs Apache-2.0 text, a NOTICE naming Google/MediaPipe/TensorFlow, and a manifest row. The general-segmentation feature was added for 0.2.3, so the file is now a shipped runtime download; that was a product choice, not a licence obstacle.
- **Adoption (done in 0.2.3):** the Apache-2.0 text is staged at [mediapipe-deeplab-v3-LICENSE.txt](./licenses/mediapipe-deeplab-v3-LICENSE.txt); the app `NOTICE.txt` line naming TensorFlow/MediaPipe is in place, and the app manifest carries the `DOWNLOAD` entry (see §0.1).


### 2.2 MODNet artifact evidence (added 2026-11-22)

The 0.2.3 release adds a download artifact that does not come from Google's bucket. Full record: [`licenses/modnet-LICENSE-EVIDENCE.md`](./licenses/modnet-LICENSE-EVIDENCE.md).

- **Artifact.** `modnet.tflite` (LiteRT conversion), **26,166,776 B**, sha256 `f7d1e9eca9562abddb60e4b0a3ed0c29441ef09cd621954978f1613914cc64bc`, downloaded 2026-11-22 from https://huggingface.co/litert-community/MODNet-LiteRT/resolve/main/modnet.tflite. HF repo commit `4d5bd22bde15c8ae5e215ca4799513844979a431`, `lastModified` 2026-09-08T08:52:54Z; the HF API's LFS `sha256` equals the local hash. Registered in the app manifest as `modnet-portrait-matting-512-f32`, `distribution: "DOWNLOAD"`, `verification.verifiedAt: 2026-11-22`.
- **Licence — upstream.** https://github.com/ZHKKKe/MODNet: GitHub licensing API `license.spdx_id: "Apache-2.0"`; `LICENSE` is the Apache-2.0 text, **11,357 B**, git blob sha `261eeb9e9f8b2b4b0d119366dda99c6fd7d35c64`, raw sha256 `c71d239d…` (saved as [`licenses/modnet-LICENSE.txt`](./licenses/modnet-LICENSE.txt)); the repository has **no NOTICE file** in its root.
- **Licence — conversion.** The HF model card (front matter) declares `license: apache-2.0` and attributes the weights to ZHKKKe/MODNet (Apache-2.0, ~6.5 M params). The card documents that the conversion was made with **litert-torch** and carries **two GPU re-authoring patches** (SE block `Linear` → 1×1 conv; fp16-safe hierarchical-mean `InstanceNorm`) — the distributed graph is a **modified derivative** of the original weights. The HF repo itself ships no separate `LICENSE` file.
- **Why this is enough for Apache-2.0 distribution.** The copyright holder released the model under Apache-2.0 (repository declaration) and the conversion publisher declares the same licence; the app downloads the published file unmodified and credits authors, paper and conversion in `NOTICE.txt` (model entry 6) and the licence screen. Apache-2.0 §4(a) copy travels in the app (`LICENSE-Apache-2.0.txt`); §4(d) no NOTICE exists upstream; §6 no marks are used. Residual: training-data provenance not evidenced (`licenses/modnet-LICENSE-EVIDENCE.md` §4).

---


## 3. Tensor specification (build-critical)

**Integration approach:** the AI studio runs **TensorFlow Lite / LiteRT only, with no MediaPipe dependency**, so the app runs the `.tflite` files **directly on LiteRT/TFLite** and reproduces the preprocessing/postprocessing described below. (MediaPipe's own `ImageSegmenter` task would do all of this automatically and is kept here as the reference implementation of these rules — switching to it later would be a dependency swap, not a model change.) The spec below is exactly what the **model** expects.

### 3.1 Summary table

| Model file | Input tensor | Input dtype / layout | Colour order | Input normalization | Output tensor | Output activation | App threshold | Channels / labels |
|---|---|---|---|---|---|---|---|---|
| `selfie_segmenter.tflite` | `[1, 256, 256, 3]` | float32, NHWC | **RGB** | `(p - 0.0) / 255.0` → **[0.0, 1.0]** | `[1, 256, 256, 1]` | **NONE** (graph output already person confidence) | **0.5** (binary mask) | 1 = person confidence |
| `selfie_segmenter_landscape.tflite` | `[1, 144, 256, 3]` (H=144, W=256) | float32, NHWC | **RGB** | `(p - 0.0) / 255.0` → **[0.0, 1.0]** | `[1, 144, 256, 1]` | **NONE** | **0.5** | 1 = person confidence |
| `selfie_multiclass_256x256.tflite` | `[1, 256, 256, 3]` | float32, NHWC | **RGB** | `(p - 127.5) / 127.5` → **[-1.0, 1.0]** | `[1, 256, 256, 6]` | **SOFTMAX** (applied by MediaPipe) | **argmax** (no global threshold) | 6 = background, hair, body-skin, face-skin, clothes, others/accessories |
| `hair_segmenter.tflite` | `[1, 512, 512, 4]` | float32, NHWC | **RGBA** (RGB used; **alpha forced to 0** by MediaPipe) | `(p - 0.0) / 255.0` → **[0.0, 1.0]** (RGB channels) | `[1, 512, 512, 2]` | **SOFTMAX** | **argmax** (0=background, 1=hair) | 2 = background, hair |
| `deeplab_v3.tflite` *(runtime download in 0.2.3)* | `[1, 257, 257, 3]` | float32, NHWC | RGB | `(p - 127.5) / 127.5` → [-1.0, 1.0] | `[1, 257, 257, 21]` | **SOFTMAX** | argmax | 21 VOC classes |
| `modnet.tflite` *(runtime download in 0.2.3)* | `[1, 3, 512, 512]` | float32, **NCHW** | RGB | `(p / 255.0 - 0.5) / 0.5` → [-1.0, 1.0] | `[1, 1, 512, 512]` | **NONE** (graph tail includes LOGISTIC; raw output is alpha in [0,1]) | 0.5 (app-level soft matte; not model metadata) | 1 = alpha |

Notes:
- **Resize policy:** MediaPipe's image segmenter preprocessor **stretches the whole frame to the model input size** (`keep_aspect_ratio` is left unset = false in the segmenter path, so no letterbox padding), then converts uint8 RGB to the float range above. Do not pad or crop when reproducing the pipeline.
- **Output masks:** MediaPipe returns **pixel-aligned masks at the input image resolution**, not at model resolution: it applies the activation per pixel then **bilinearly resizes** each confidence mask back to the source image. Confidence masks are float32 in **[0.0, 1.0]**; the `categoryMask` is a **uint8** image where each pixel is the winning class index (and for a single-channel binary model, MediaPipe derives it as `confidence > 0.5 ? 1 : 0`).
- **Logits warning (direct TFLite integration):** the exported `.tflite` graphs for **multiclass, hair and deeplab do NOT contain the softmax** — the metadata tells the consumer to apply `SOFTMAX`, and MediaPipe's `TensorsToSegmentationCalculator` applies it *outside* the model (verified in source, lines 204-214/259-264). Running raw TFLite, the app must **apply softmax across the channel axis itself** before thresholding/argmax; the selfie square/landscape models need no post-activation. Missing this step makes mask values look like arbitrary scores instead of probabilities.
- **Thresholds** are **not model metadata**; 0.5 for binary and argmax for multi-class are MediaPipe's own mask-derivation rules. A UI slider (e.g. 0.3–0.7 edge softness) is an app-level choice and should be labelled as such.
- **Mask alpha for compositing:** use the confidence mask directly as alpha (no re-normalization) for soft edges; clamp to [0,1].

### 3.2 Provenance of every spec field

| Spec field | Where it comes from (official) |
|---|---|
| Tensor names / shapes / dtype (all models) | Read from each `.tflite` flatbuffer on 2026-11-16 (input/output tensors: `input_1`/`activation_10`, `input_29`/`Identity`, `input_1`/`conv2d_transpose_4`, `sub_7`/`ResizeBilinear_3`). TFLite schema: <https://github.com/tensorflow/tensorflow/blob/v2.16.1/tensorflow/lite/schema/schema.fbs> |
| Input normalization (mean/std) | Embedded TFLite Metadata `NormalizationOptions` (mean/std) read from the model files: selfie/landscape/hair = mean 0, std 255; multiclass/deeplab = mean 127.5, std 127.5. MediaPipe applies `output_tensor_float_range = [(0-mean)/std, (255-mean)/std]` — [image_preprocessing_graph.cc](https://github.com/google-ai-edge/mediapipe/blob/master/mediapipe/tasks/cc/components/processors/image_preprocessing_graph.cc) lines 86-110. |
| Colour order RGB | Selfie model card: *"Channels order: RGB with values in [0.0, 1.0]"*; Multiclass card: *"normalized RGB image"*; MediaPipe `ImageSegmenter` documents RGB input. |
| Input sizes | Model cards (selfie: 256x256 / landscape 144x256; multiclass: [256,256,3]) + docs models table (<https://ai.google.dev/edge/mediapipe/solutions/vision/image_segmenter>: SelfieSegmenter 256x256 & 144x256, HairSegmenter 512x512, SelfieMulticlass 256x256, DeepLab-V3 257x257). |
| Output activation (NONE / SOFTMAX) | Embedded `SEGMENTER_METADATA` custom metadata (flatbuffer id `V001`; schema [image_segmenter_metadata_schema.fbs](https://github.com/google-ai-edge/mediapipe/blob/master/mediapipe/tasks/metadata/image_segmenter_metadata_schema.fbs)): selfie square & landscape = field absent = **NONE**; multiclass, hair, deeplab = **SOFTMAX**. MediaPipe reads this in [image_segmenter_graph.cc](https://github.com/google-ai-edge/mediapipe/blob/master/mediapipe/tasks/cc/vision/image_segmenter/image_segmenter_graph.cc) (`GetImageSegmenterOptions(...)->activation()`). |
| Activation semantics, 0.5 threshold, argmax, bilinear resize | [tensors_to_segmentation_calculator.cc](https://github.com/google-ai-edge/mediapipe/blob/master/mediapipe/tasks/cc/vision/image_segmenter/calculators/tensors_to_segmentation_calculator.cc): `Sigmoid`/`StableSoftmax`/`activation_fn`; `pixel = confidence_scores[0] > 0.5f ? 0 : kUnLabeledPixelValue` for the binary category mask; argmax for multi-channel; `BilinearInterpolate` + resize to input image size. |
| Confidence mask range [0,1] and mask types | Docs page "Output": *"outputs per-category floating-point confidence masks in [0.0, 1.0]"*, *"uint8 category mask where each pixel value indicates the winning class index"*, and masks are "pixel-aligned … matching the dimensions of the input image". |
| Hair 4-channel alpha=0 handling | [image_segmenter_graph.cc](https://github.com/google-ai-edge/mediapipe/blob/master/mediapipe/tasks/cc/vision/image_segmenter/image_segmenter_graph.cc) `ConvertImageToTensors` (channel==4 legacy path; `SetAlphaCalculator` with `alpha_value 0`) and `ConfigureImageTransformationCalculator`. |
| Resize = stretch, no letterbox | [image_to_tensor_calculator.proto](https://github.com/google-ai-edge/mediapipe/blob/master/mediapipe/calculators/tensor/image_to_tensor_calculator.proto) `keep_aspect_ratio` comment (false → "image region is stretched to fill output tensor fully"); the segmenter's `ConfigureImageToTensorCalculator` never sets it. |
| Classes: binary 0=background/1=person; multiclass 6 names; hair 2 names | Docs models table (§ "Models") — listed verbatim in §3.1. |

---

## 4. Runtime / AAR licences

| Artifact | Latest version & date (verified) | Licence | Size / packaging notes |
|---|---|---|---|
| `com.google.mediapipe:tasks-vision` | **1.2.3**; Google Maven metadata updated 2026-11-11, AAR Last-Modified 2026-11-11 23:21 UTC | **Apache-2.0** — POM `<license><name>The Apache Software License, Version 2.0</name><url>http://www.apache.org/licenses/LICENSE-2.0.txt</url>` | AAR = **486,255 B**; contains `classes.jar` (277 KB), `AndroidManifest.xml`, `R.txt` and `META-INF/NOTICE` (**3.28 MB**, third-party attributions — must be surfaced in the app's OSS licences screen if this AAR is ever used). POM deps: androidx.annotation 1.1.0, guava 27.0.1-android, tasks-core 1.2.3; optional litertlm-android 0.18.0. **No native `.so` inside the AAR** → the LiteRT runtime is a separate artifact (§7). **Not used in 0.2.3** — the app has no MediaPipe dependency. |
| `com.google.ai.edge.litert:litert` (LiteRT, successor of TensorFlow Lite) | **2.3.2**; Google Maven updated 2026-10-26 | **Apache-2.0** (POM) | AAR = **7,640,090 B** (incl. native libs). `litert-api` 2.3.2 dependency. **AAR root ships `LICENSE` (13,575 B, sha256 `71c6915d…`) and `THIRD_PARTY_NOTICE.txt` (1,960,672 B, sha256 `db6e1ff4…`; 35,719 lines)** — AGP does **not** copy AAR root files into the APK, so neither is packaged by AGP (verified 2026-11-22). **In the 18:34Z build:** both AAR-root files ship verbatim as `assets/models/LiteRT-LICENSE.txt` and `assets/models/LiteRT-THIRD_PARTY_NOTICE.txt`, embedded in APK/AAB and opened from the licences screen. |
| `org.tensorflow:tensorflow-lite` | **2.17.0** (last release; Maven Central metadata lastUpdated 2025-01-09) | **Apache-2.0** (POM) | POM declares Gradle **relocation → `com.google.ai.edge.litert:litert:1.0.1`**. Do not start new code on this coordinate; use LiteRT. |
| `com.microsoft.onnxruntime:onnxruntime-android` | **1.32.0**; Maven Central metadata updated 2026-11-13 | **MIT** (POM; repo LICENSE = MIT, Copyright (c) Microsoft Corporation) | AAR (not measured here); only needed if we adopt ONNX-converted PyTorch models (BiRefNet/Real-ESRGAN) after MVP; MODNet ships as a pre-built LiteRT conversion (§2.2). |
| MediaPipe repo / model code | master as of 2026-11-16 | Apache-2.0 | <https://github.com/google-ai-edge/mediapipe/blob/master/LICENSE> |
| MediaPipe docs page content | last updated 2026-11-11 UTC | Docs text CC BY 4.0; code samples Apache-2.0 (not a model licence) | <https://ai.google.dev/edge/mediapipe/solutions/vision/image_segmenter> |

**Recommendation for a Compose app:** use **LiteRT 2.3.2** (Apache-2.0) directly — it is the maintained TensorFlow Lite runtime and the only inference dependency in the AI studio. Implement preprocessing/postprocessing from §3 in Kotlin (stretch resize → uint8 RGB → float normalization → inference → optional softmax → confidence mask → resize/composite). Do **not** add the MediaPipe `tasks-vision` AAR: the AI studio has no MediaPipe dependency, and the AAR we inspected does not even package the native runtime (486 KB, no `.so`), so it would have to be paired with LiteRT anyway. ONNX Runtime 1.32.0 (MIT) stays out of the shipped app — it is only the migration path for post-MVP PyTorch-lineage models (BiRefNet/Real-ESRGAN). MODNet already ships in 0.2.3 as a pre-built LiteRT conversion (§2.2), so it does not need ONNX.

**Runtime in 0.2.3 (final build 2026-11-22T18:34Z).** `app/build.gradle.kts` declares `com.google.ai.edge.litert:litert:2.3.2` and
nothing else for inference (versionCode 5 / versionName "0.2.3"). The TensorFlow Lite 2.16.1 dependency assumed by the 0.1.0
draft is gone; its 2.17.0 successor on Maven Central is a relocation stub to LiteRT, so there is no reason to reintroduce it.
Verified in the final release APK (22,072,532 B, sha256 `aa7674d5…`) and AAB (15,480,566 B, sha256 `5bf7b419…`): `libLiteRt.so` and
`liblitert_jni.so` are present per ABI and **no** `libtensorflowlite_jni.so`; `assets/models/` holds the two bundled models,
`LICENSE-Apache-2.0.txt` (12,331 B), `NOTICE.txt` (6,512 B, `2fe0d5f0…`), `LiteRT-LICENSE.txt` (13,575 B), `LiteRT-THIRD_PARTY_NOTICE.txt`
(1,960,672 B) and `manifest.json` (23,925 B, six entries). The earlier 17:36–17:42Z pair with the old 3,074 B NOTICE is superseded; the root-level convenience
copies were replaced with these 18:34Z outputs and verified byte-identical on 2026-11-22T21:47+03:00.
Apache-2.0 obligations are unchanged by the rename: ship the licence text and the NOTICE attributions, reproduce the LiteRT
`THIRD_PARTY_NOTICE.txt` (done in the 18:34Z build), and do not use "TensorFlow", "LiteRT", "MediaPipe" or "MODNet" as a product brand or
imply endorsement.

---

## 5. Commercial-use and redistribution obligations

1. **Apache-2.0 compliance is light but real:** ship a copy of the licence (done: `licenses/<id>-LICENSE.txt`, verbatim from the MediaPipe repo), preserve copyright/attribution notices, and if the distributed artifact includes a NOTICE file, reproduce its attributions. We do not modify the bundled or DeepLab files, so no "modified files" notice arises for them; **MODNet's LiteRT conversion is a modified derivative published by `litert-community`** and we download it unmodified — no §4(b) duty for us, but the NOTICE should state the conversion modifications for transparency, and any future re-hosting must carry `licenses/modnet-model-card-NOTICE.txt`. Apache-2.0 §3 grants patents; §6 does **not** grant trademark rights — do not imply Google/MediaPipe endorsement or use "MediaPipe" as a product brand.
2. **Reproduce the LiteRT third-party notices — done in the 18:34Z rebuild (2026-11-22).** The LiteRT 2.3.2 AAR ships `LICENSE` (13,575 B, sha256 `71c6915d…`) and `THIRD_PARTY_NOTICE.txt` (1,960,672 B, sha256 `db6e1ff4…`, 35,719 lines, starts "Abseil:") at the **AAR root** — *not* `META-INF/NOTICE` — and AGP does not package AAR root files. The app now ships both files verbatim as `assets/models/LiteRT-LICENSE.txt` and `assets/models/LiteRT-THIRD_PARTY_NOTICE.txt` (byte-compared with the AAR root), embedded in the rebuilt APK/AAB and opened from the licences screen. The MediaPipe `tasks-vision` NOTICE matters only if that AAR is used, which it is not.
3. **Model cards are the licence evidence.** Archive the model-card PDFs/HTML cards (selfie, multiclass, hair; the DeepLab-V3 LiteRT mirror card; the MODNet conversion card — URLs in §8) alongside the source snapshot in case Google or Hugging Face removes them; the MediaPipe `.tflite` files embed an **empty** licence field. For MODNet the evidence is split: the upstream repository's Apache-2.0 declaration + `LICENSE`, and the conversion card's `license: apache-2.0` front matter.
4. **Intended-use limits are not licence terms but should shape the product/EULA:** the MediaPipe model cards (and the DeepLab-V3 / MODNet cards) explicitly put **surveillance, identity recognition and human life-critical decisions out of scope**, and describe the intended uses as AR / video conferencing / entertainment. The editor must not market an "identity recognition" or "surveillance" feature; background effects and cutout are in scope.
5. **Runtime downloads:** pin the versioned URL, verify SHA-256 before use, store in app-private storage, and only download the 16.4 MB multiclass model on Wi-Fi/unmetered by default. If a downloaded model fails the hash, abort and fall back to the bundled segmenter. 0.2.3 adds DeepLab-V3 (2,780,176 B, Google storage) and MODNet (26,166,776 B, Hugging Face) — apply the same pinning/hash rule to both; the app manifest carries `downloadUrl` + `sha256` + `verification.verifiedAt: 2026-11-22` for each. Google may delete/rotate bucket objects — a CI check that fetches hashes monthly is advisable, and the same applies to the two HF repositories.
6. **Model binaries and where they live:** the **two bundled** `.tflite` files, `NOTICE.txt`, `LICENSE-Apache-2.0.txt` and the two LiteRT notice/licence files (`LiteRT-LICENSE.txt`, `LiteRT-THIRD_PARTY_NOTICE.txt`) are app assets under `app/src/main/assets/models/`; the **four download** models are fetched at runtime from their pinned URLs (DeepLab-V3 from Google storage, MODNet from Hugging Face) and are **not** committed. `docs/product/` keeps only the manifest + licence texts/evidence. `model_manifest.json` is written to be copied to `app/src/main/assets/models/manifest.json`; each entry carries `runtime` (`tflite`), `assetPath`, `sourceUrl`/`downloadUrl` (GCS path pinned to model version 1; MODNet uses the HF commit-pinned URL `resolve/4d5bd22b…` with a SHA-256 integrity pin (`latestAliasUrl` keeps the `main` alias for reference only)), `sha256`, `sizeBytes` and `verification.verifiedAt`. Unknown fields are ignored by the parser.
7. **Verification records.** Each catalogue model has a licence name, evidence URL, fetch date and artifact SHA-256 recorded here and in [`model_manifest.json`](./model_manifest.json). SHA-256 values: selfie_segmenter.tflite `191ac952…`; selfie_segmenter_landscape.tflite `490e9ea7…`; selfie_multiclass_256x256.tflite `c6748b12…`; hair_segmenter.tflite `2628cf3c…`; deeplab_v3.tflite `ff36e24d…`; modnet.tflite `f7d1e9ec…` (full hashes in §2). A runtime download is also our own distribution, so it needs the same Apache-2.0 text + attribution as a bundled file. The in-app licence screen names each download model + publisher + licence (and the Tkachenka citation for hair) and surfaces the LiteRT third-party notice.
8. **Bundled-asset state verified against the rebuilt app (2026-11-22T18:34Z).** `app/src/main/assets/models/` contains `LICENSE-Apache-2.0.txt` (12,331 B, sha256 `8707eef0…`; MediaPipe's Apache-2.0 copy including the ~973 B Lucent Technologies / Rob Pike–Ken Thompson appendix), `NOTICE.txt` (**6,512 B**, sha256 `2fe0d5f0…` — references both LiteRT files, covers all six models, network/storage and intended-use notes), `LiteRT-LICENSE.txt` (13,575 B, `71c6915d…`), `LiteRT-THIRD_PARTY_NOTICE.txt` (1,960,672 B, `db6e1ff4…`), `manifest.json` (23,925 B, sha256 `9fde9460…`, byte-identical to `docs/product/model_manifest.json`; six entries, MODNet author corrected to Qiong Yan and URLs pinned to the HF commit `4d5bd22b…`) and the two bundled `.tflite` files (hashes match §2). The rebuilt APK/AAB embed the same bytes (verified). Licence-screen residuals: per-model source URLs are still not shown in the rows and there is no verification footer — neither is a legal duty.
9. **Release-artifact staleness — resolved by the 18:34Z rebuild (2026-11-22).** The final APK (22,072,532 B, sha256 `aa7674d5…`) and AAB (15,480,566 B, sha256 `5bf7b419…`) embed the current 6,512 B NOTICE (`2fe0d5f0…`), manifest 23,925 B (`9fde9460…`) and both LiteRT assets (extracted and re-hashed). **Residual release-process hazard — resolved 2026-11-22T21:47+03:00:** the root-level convenience files `Midad-Studio-0.2.3-release.apk/.aab` (project root) and the copies one level up were replaced with these exact outputs and verified byte-identical (22,072,532 B `aa7674d5…` / 15,480,566 B `5bf7b419…`); no artifact carrying the old 3,074 B NOTICE remains in the project root.
10. **In-app licence screen performs the attribution duty (done in the 18:34Z build).** `LicensesScreen.kt` opens a full-screen licence-text dialog reading `assets/models/*` (chunked LazyColumn for the 1.9 MB third-party notice), with rows for `NOTICE.txt`, `LICENSE-Apache-2.0.txt`, `LiteRT-LICENSE.txt` and `LiteRT-THIRD_PARTY_NOTICE.txt`; the strings no longer claim texts "ship" without a way to read them; LiteRT, DeepLab-V3 and MODNet rows carry corrected notes. Verified against the rebuilt APK/dex/resources and by code inspection. Open residuals (not release blockers): per-model source/evidence URLs are not surfaced in the rows, and there is no "verified 2026-11-22" footer.

---

## 6. Shipped model set for 0.2.3

0.2.3 ships **six entries** in `app/src/main/assets/models/manifest.json`:

- **Bundled (2):** MediaPipe **SelfieSegmenter square** (256x256 f16, 249,537 B) — default person/background cutout, blur and replace; runs live on CPU (~33 ms on Pixel 6 per Google's benchmark). **SelfieSegmenter landscape** (256x144 f16, 250,177 B) — only used for a fixed landscape video canvas.
- **Runtime downloads (4):** **SelfieMulticlass 256x256 f32** (16,371,837 B) — 6-class hair-aware "HD" cutout, ~71 ms GPU / 218 ms CPU per Pixel 6 frame; **HairSegmenter 512x512 f32** (781,618 B) — single-person selfie hair mask (cite Tkachenka et al., CVPRW 2019); **DeepLab-V3 257x257 f32** (2,780,176 B) — 21-class general objects/scenery; **MODNet-LiteRT 512x512 f32** (26,166,776 B) — soft portrait matte. All four are hash-verified on download and stored app-private.

Face enhance / upscale / style transfer remain out of scope: every viable GAN restoration model was non-commercial (CodeFormer, RMBG, AnimeGANv2) or licence-contaminated by its training stack (GFPGAN → NVIDIA StyleGAN2). Real-ESRGAN (BSD-3, upscale) and BiRefNet (MIT, higher-quality matting) remain unimplemented options, not licence blockers. The model cards' intended-use limits still apply: no surveillance, identity recognition or life-critical decisions; background effects and cutout are in scope.

**Attribution (mandatory):** the shipped `NOTICE.txt` + in-app licence screen + Apache-2.0 text; the screen must name each download model, its publisher and licence, and surface the LiteRT `THIRD_PARTY_NOTICE.txt` (done in the 2026-11-22T18:34Z build: asset + licence-screen entry).

---

## 7. Open questions / uncertainties (UNRESOLVED)

1. **LiteRT dependency and APK size.** The AI studio uses TensorFlow Lite / LiteRT only; the integration had to confirm `com.google.ai.edge.litert:litert:2.3.2` resolves on the configured AGP/Compose toolchain, that its native ABI splits (7.6 MB AAR) land in the APK/AAB, and measure the resulting size delta. A `tasks-vision` route would need LiteRT explicitly (its AAR is only 486 KB, no native libs). **Resolved in 0.2.3 (2026-11-22):** LiteRT 2.3.2 resolves and ships its native libs (`libLiteRt.so`, `liblitert_jni.so` per ABI); the signed release APK is 22,072,532 B and the AAB 15,480,566 B (18:34Z rebuild). Items 2 and 8–10 were resolved in the 18:34Z rebuild, and the root-level distribution copies were replaced with those outputs and verified byte-identical on 2026-11-22T21:47+03:00.
2. **DeepLab-V3 weights licence — resolved 2026-11-16; shipped as a runtime download in 0.2.3 (2026-11-22).** The record was independently verified: Kaggle Models API licenseName "Apache 2.0" on both TF Lite instances of tensorflow/deeplabv3; no MediaPipe model card exists (negative evidence in §2.1 and the evidence file), but the official LiteRT mirror card now states `license: apache-2.0` (repo commit `739202fd…`, LFS sha256 matches the 2,780,176 B file). The licence record is complete. Caveats: the Kaggle evidence is an API field (not a rendered licence panel); the MediaPipe build differs from the Kaggle metadata-v2 instance by 125 B, so it is a modified distribution permitted by Apache-2.0. In 0.2.3 it is a 2.78 MB runtime download with a NOTICE entry and a manifest row — no longer a disabled product entry.
3. **Model-card availability is external.** The licence evidence is PDFs/HTML cards on `storage.googleapis.com/mediapipe-assets` and Hugging Face; archive them locally (outside the repository) as part of release compliance, including the DeepLab-V3 mirror card and the MODNet conversion card.
4. **Model download hosting.** Shipping/downloading weights is our own distribution: our app must carry the Apache-2.0 licence + attributions (files prepared). If we later use Play Asset Delivery, the same obligations apply.
5. **Real-device performance/battery** for multiclass at 16 MP stills (tile/resize strategy) is not yet benchmarked; only Google's Pixel 6 pipeline numbers exist.
6. **Training-data provenance — residual, not a distribution blocker for MODNet.** U2-Net/DUTS, BiRefNet/DIS, Real-ESRGAN and MODNet were not re-verified at the data level. For **MODNet**, 0.2.3 ships the authors' released weights under Apache-2.0 and the conversion under its card's Apache-2.0; the training corpus was not evidenced in this review (recorded in `licenses/modnet-LICENSE-EVIDENCE.md` §4). U2-Net remains unresolved (no retrievable DUTS licence) and is not adopted.
7. **Hair segmenter is only for single-person selfies** (model card limitations: >1.5 m distance, multiple people, occlusions degrade); do not advertise it as general hair segmentation for group photos.
8. **Do not trust the docs page for licence** — its footer licence (CC BY 4.0) covers the page text, not the models; model cards are the weights-licence evidence.
9. **Models installed from outside our catalogue are licensed by their publisher — UNRESOLVED by design.** The 0.2.3 model manager only offers the entries in `assets/models/manifest.json`, each with a pinned URL and SHA-256, so in the shipped configuration every model is one we catalogued and verified. If a future version lets a user import an arbitrary `.tflite` file, that file is the user's responsibility and its licence is the publisher's: we cannot verify its provenance, must not relicense it, must not present it as "licensed for commercial use", and the licence screen must distinguish it from the bundled and catalogue-downloaded models. Keep any such import path out of a release until this is written down.

10. **LiteRT third-party notice — resolved in the 18:34Z rebuild (2026-11-22).** LiteRT 2.3.2's AAR root contains `LICENSE` (13,575 B) and `THIRD_PARTY_NOTICE.txt` (1,960,672 B, 35,719 lines), which AGP does not package. Apache-2.0 §4(d) requires the attributions to travel: the app now ships both AAR-root files verbatim as `assets/models/LiteRT-LICENSE.txt` and `assets/models/LiteRT-THIRD_PARTY_NOTICE.txt` (byte-compared with the AAR root, embedded in the APK/AAB — extracted and re-hashed 2026-11-22) and opens them from the licences screen.

---

## 8. Source index (fetched 2026-11-16; 0.2.3 additions 2026-11-22)

**MediaPipe official**
- Image Segmenter docs (models table, output types, benchmarks; footer "Last updated 2026-11-11 UTC"): <https://ai.google.dev/edge/mediapipe/solutions/vision/image_segmenter>
- Selfie Segmentation model card PDF (May 6, 2021; Apache-2.0): <https://storage.googleapis.com/mediapipe-assets/Model%20Card%20MediaPipe%20Selfie%20Segmentation.pdf>
- Multiclass Segmentation model card PDF (May 10, 2023; Apache-2.0): <https://storage.googleapis.com/mediapipe-assets/Model%20Card%20Multiclass%20Segmentation.pdf>
- Hair Segmentation model card PDF (2019-01-14; Apache-2.0): <https://storage.googleapis.com/mediapipe-assets/Model%20Card%20-%20Hair%20Segmentation.pdf>
- Model files (pinned version `/1/`, see §2 table) and `/latest/` aliases under <https://storage.googleapis.com/mediapipe-models/image_segmenter/>
- Kaggle Models licence record for DeepLab-V3 (TF Lite deployments of tensorflow/deeplabv3; licenseName "Apache 2.0"): <https://www.kaggle.com/api/v1/models/list?pageSize=20&search=deeplab> (JSON API, HTTP 200, 152,613 B, fetched 2026-11-16); model page <https://www.kaggle.com/models/tensorflow/deeplabv3>
- DeepLab-V3 licence record re-check (2026-11-16): the API was re-fetched and the artifact re-downloaded; matched 2,780,176 B and sha256 ff36e24d40547fe9e645e2f4e8745d1876d6e38b332d39a82f0bf0f5d1d561b3.
- DeepLab-V3 official LiteRT mirror card (Hugging Face, fetched 2026-11-22): <https://huggingface.co/litert-community/MediaPipe-ImageSegmenter-DeepLab-V3> — card front matter `license: apache-2.0`; repo commit `739202fd…`, lastModified 2026-11-19; file LFS sha256 `ff36e24d40547fe9e645e2f4e8745d1876d6e38b332d39a82f0bf0f5d1d561b3`, 2,780,176 B (= our artifact). Analysis: `licenses/mediapipe-deeplab-v3-LICENSE-EVIDENCE.md` §6.
- MediaPipe source: [LICENSE](https://github.com/google-ai-edge/mediapipe/blob/master/LICENSE), [image_segmenter_graph.cc](https://github.com/google-ai-edge/mediapipe/blob/master/mediapipe/tasks/cc/vision/image_segmenter/image_segmenter_graph.cc), [image_preprocessing_graph.cc](https://github.com/google-ai-edge/mediapipe/blob/master/mediapipe/tasks/cc/components/processors/image_preprocessing_graph.cc), [tensors_to_segmentation_calculator.cc](https://github.com/google-ai-edge/mediapipe/blob/master/mediapipe/tasks/cc/vision/image_segmenter/calculators/tensors_to_segmentation_calculator.cc), [image_to_tensor_calculator.proto](https://github.com/google-ai-edge/mediapipe/blob/master/mediapipe/calculators/tensor/image_to_tensor_calculator.proto), [image_segmenter_metadata_schema.fbs](https://github.com/google-ai-edge/mediapipe/blob/master/mediapipe/tasks/metadata/image_segmenter_metadata_schema.fbs), [TFLite metadata_schema.fbs (tflite-support)](https://github.com/tensorflow/tflite-support/blob/master/tensorflow_lite_support/metadata/metadata_schema.fbs)
- MediaPipe Tasks Vision Maven metadata/POM: <https://dl.google.com/dl/android/maven2/com/google/mediapipe/tasks-vision/maven-metadata.xml>, <https://dl.google.com/dl/android/maven2/com/google/mediapipe/tasks-vision/1.2.3/tasks-vision-1.2.3.pom>, AAR: <https://dl.google.com/dl/android/maven2/com/google/mediapipe/tasks-vision/1.2.3/tasks-vision-1.2.3.aar>
- LiteRT Maven metadata/POM: <https://dl.google.com/dl/android/maven2/com/google/ai/edge/litert/litert/maven-metadata.xml>, <https://dl.google.com/dl/android/maven2/com/google/ai/edge/litert/litert/2.3.2/litert-2.3.2.pom>, AAR: <https://dl.google.com/dl/android/maven2/com/google/ai/edge/litert/litert/2.3.2/litert-2.3.2.aar>
- LiteRT 2.3.2 AAR root `LICENSE` (13,575 B, sha256 `71c6915d…`) and `THIRD_PARTY_NOTICE.txt` (1,960,672 B, sha256 `db6e1ff4…`; 35,719 lines) — extracted 2026-11-22 from the Gradle-cache AAR `litert-2.3.2.aar` (7,640,090 B); AGP does not package AAR root files.
- TensorFlow Lite Maven: <https://repo1.maven.org/maven2/org/tensorflow/tensorflow-lite/maven-metadata.xml>, POM 2.17.0: <https://repo1.maven.org/maven2/org/tensorflow/tensorflow-lite/2.17.0/tensorflow-lite-2.17.0.pom>

**Other models / runtimes**
- ONNX Runtime: Maven metadata <https://repo1.maven.org/maven2/com/microsoft/onnxruntime/onnxruntime-android/maven-metadata.xml>, POM 1.32.0 <https://repo1.maven.org/maven2/com/microsoft/onnxruntime/onnxruntime-android/1.32.0/onnxruntime-android-1.32.0.pom>, repo LICENSE <https://github.com/microsoft/onnxruntime/blob/main/LICENSE>
- U2-Net: <https://github.com/xuebinqin/U-2-Net> (LICENSE Apache-2.0 via GitHub API; README weight links)
- MODNet (upstream): <https://github.com/ZHKKKe/MODNet> — GitHub licensing API `spdx_id: Apache-2.0`; `LICENSE` 11,357 B, blob `261eeb9e…`, raw sha256 `c71d239d…`; no NOTICE file. Paper: Ke et al., "MODNet: Real-Time Trimap-Free Portrait Matting via Objective Decomposition", AAAI 2022.
- MODNet LiteRT conversion (fetched 2026-11-22): card <https://huggingface.co/litert-community/MODNet-LiteRT> — front matter `license: apache-2.0`; repo commit `4d5bd22bde15c8ae5e215ca4799513844979a431`, lastModified 2026-09-08T08:52:54Z; file <https://huggingface.co/litert-community/MODNet-LiteRT/resolve/main/modnet.tflite> LFS sha256 `f7d1e9eca9562abddb60e4b0a3ed0c29441ef09cd621954978f1613914cc64bc`, 26,166,776 B; card documents two GPU re-authoring patches. Analysis: `licenses/modnet-LICENSE-EVIDENCE.md`.
- BiRefNet: <https://github.com/ZhengPeng7/BiRefNet> (LICENSE MIT); HF <https://huggingface.co/ZhengPeng7/BiRefNet> (license: mit)
- RMBG-1.4: <https://huggingface.co/briaai/RMBG-1.4>; RMBG-2.0: <https://huggingface.co/briaai/RMBG-2.0> (non-commercial BRIA licences)
- Real-ESRGAN: <https://github.com/xinntao/Real-ESRGAN> (LICENSE BSD-3-Clause)
- GFPGAN: <https://github.com/TencentARC/GFPGAN> (LICENSE: Apache-2.0 except third-party; NVIDIA StyleGAN2 non-commercial, DFDNet CC BY-NC-SA)
- CodeFormer: <https://github.com/sczhou/CodeFormer> (S-Lab License 1.0, non-commercial)
- AnimeGANv2: <https://github.com/TachibanaYoshino/AnimeGANv2> (README non-commercial; no LICENSE file), port <https://github.com/bryandlee/animegan2-pytorch> (MIT code)
- SAM: <https://github.com/facebookresearch/segment-anything> (Apache-2.0); SAM 2: <https://github.com/facebookresearch/sam2> (Apache-2.0)
- Datasets: DIS <https://github.com/xuebinqin/DIS> (Apache-2.0), P3M <https://github.com/JizhiziLi/P3M> (MIT)

**Licence copies saved in this repo:** `docs/product/licenses/`
- `mediapipe-selfie-segmenter-LICENSE.txt`, `mediapipe-selfie-segmenter-landscape-LICENSE.txt`, `mediapipe-selfie-multiclass-LICENSE.txt`, `mediapipe-hair-segmenter-LICENSE.txt`, `mediapipe-deeplab-v3-LICENSE.txt` — Apache License 2.0 text as distributed in the MediaPipe repository, 12,331 B, SHA-256 `8707eef0533987efc5b155d64761eeb6e20793f50b9bd1a68dad1cf4719d0ed8` (includes the ~973 B Lucent Technologies / Rob Pike–Ken Thompson appendix — this is MediaPipe's copy, not the canonical apache.org file).
- `mediapipe-model-cards-NOTICE.txt` — pre-0.2.3 attribution/citation text for the four MediaPipe models + runtime notices summary; **superseded for the app by `app/src/main/assets/models/NOTICE.txt`**, which now covers all six models (kept here as the per-model docs copy).
- `mediapipe-deeplab-v3-LICENSE.txt` — the same 12,331 B Apache-2.0 copy (listed above), staged for the DeepLab-V3 runtime download (shipped as a download in 0.2.3).
- `mediapipe-deeplab-v3-LICENSE-EVIDENCE.md` — model-level licence evidence record (Kaggle Models API JSON, fetch date, artifact hash, negative evidence about the missing MediaPipe model card) **plus the 2026-11-22 §6 addendum** covering the runtime download and the official LiteRT mirror card.
- `modnet-LICENSE.txt` — upstream Apache-2.0 text as served by `ZHKKKe/MODNet` (11,357 B, SHA-256 `c71d239df91726fc519c6eb72d318ec65820627232b2f796219e87dcf35d0ab4`; equals the canonical apache.org text minus its leading blank line).
- `modnet-LICENSE-EVIDENCE.md` — MODNet evidence record: upstream repo/API, HF conversion card and LFS hash, two conversion patches, obligations table, residual gaps (training data, `litert-community` affiliation not evidenced, HF repo has no LICENSE file), reproduction commands.
- `modnet-model-card-NOTICE.txt` — attribution + modification statement to carry if we ever re-host the MODNet conversion bytes ourselves (not needed while we download from HF).

---

*Prepared 2026-11-16; updated for release 0.2.3 on 2026-11-22. If any hash or licence URL changes on Google's or Hugging Face's side, treat the new artifact as unreviewed and re-run this verification before shipping it.*
