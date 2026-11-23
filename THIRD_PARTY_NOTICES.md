# Third-Party Notices

Midad Studio is built with third-party libraries and ships or downloads third-party AI models. This file
records their licences and where the evidence lives. It covers the code and models used by version **0.2.3**
(versionCode 5).

Midad Studio's own code is not open source: it is source-available under the terms in [LICENSE](LICENSE), which
reserve all rights. Nothing in that licence changes the rights you receive under the third-party licences listed
here; those components remain under their own terms.

## How this list was verified

- The runtime dependencies are the ones declared in `app/build.gradle.kts` for the 0.2.3 build. For each artifact,
  the licence identifier was checked against the published Maven POM metadata resolved into the Gradle dependency
  cache during the 0.2.3 release verification (2026-11-22); every AndroidX, Compose, Media3, LiteRT, Kotlin and
  kotlinx entry declares **Apache License 2.0**.
- The AI models are the six entries in [docs/product/model_manifest.json](docs/product/model_manifest.json).
  Their licence and provenance evidence lives in [docs/product/MODEL_LICENSES.md](docs/product/MODEL_LICENSES.md)
  and the source files under [docs/product/licenses/](docs/product/licenses/).
- Where this file or the in-app licence screen states a licence, it is the licence the project verified from the
  component's own metadata, model card or licence text. If a component's upstream licence changes, the entry is
  corrected rather than reinterpreted.

## Runtime libraries included in the app

| Component | Version | Licence | Notes |
|---|---|---|---|
| Kotlin standard library (including `kotlin-stdlib` and its JDK extensions) | 2.0.21 | [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0) | JetBrains |
| Kotlin Coroutines (`kotlinx-coroutines-core`, `kotlinx-coroutines-android`) | 1.9.0 | [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0) | JetBrains |
| AndroidX Core KTX (`androidx.core:core-ktx`) | 1.13.1 | [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0) | Google / AndroidX |
| AndroidX Lifecycle (`runtime-ktx`, `runtime-compose`, `viewmodel-compose`) | 2.8.7 | [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0) | Google / AndroidX |
| AndroidX Activity Compose | 1.9.3 | [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0) | Google / AndroidX |
| AndroidX Navigation Compose | 2.8.5 | [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0) | Google / AndroidX |
| AndroidX ExifInterface | 1.3.7 | [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0) | Google / AndroidX |
| Jetpack Compose UI, Foundation, Runtime, UI Graphics (`androidx.compose.*`) | 1.7.6 (BOM 2024.12.01) | [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0) | Google / AndroidX |
| Compose Material 3 (`androidx.compose.material3:material3`) | 1.3.1 (BOM 2024.12.01) | [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0) | Google / AndroidX |
| Compose UI Tooling Preview (annotation artifact) | 1.7.6 (BOM 2024.12.01) | [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0) | Compile-time preview annotations |
| AndroidX Media3: Transformer, Effect, Common, ExoPlayer, UI | 1.5.1 | [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0) | Google / AndroidX. The version is pinned; the full-project preview depends on the restricted `CompositionPlayer` API of 1.5.1 |
| LiteRT (`com.google.ai.edge.litert:litert`) | 2.3.2 | [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0) | Google. Runs the on-device segmentation models |
| AndroidX Graphics Path (native library, transitive) | from the Compose UI stack | [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0) | Ships `libandroidx.graphics.path.so`, one of the nine 16 KB-aligned native libraries in the APK |

The three ABIs in the release AAB (arm64-v8a, armeabi-v7a, x86_64) contain these libraries and their own
transitive AndroidX/Kotlin dependencies. Those transitive libraries are also Apache-2.0; this is the same licence
family as the direct dependencies above.

## AI models

Two models are bundled inside the APK/AAB. Four are **download-only**: they are fetched from their pinned HTTPS
URL only after an explicit user action, verified against the pinned SHA-256 before installation, and stored in
app-private storage. See [docs/product/MODEL_LICENSES.md](docs/product/MODEL_LICENSES.md) for byte sizes and the
full SHA-256 of each file.

| Model | Distribution | Size (bytes) | Licence | Upstream / model card |
|---|---|---|---|---|
| MediaPipe Selfie Segmenter (square, 256x256, float16) | Bundled | 249,537 | [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0) | [MediaPipe model card](https://storage.googleapis.com/mediapipe-assets/Model%20Card%20MediaPipe%20Selfie%20Segmentation.pdf) |
| MediaPipe Selfie Segmenter (landscape, 256x144, float16) | Bundled | 250,177 | [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0) | Same MediaPipe selfie segmentation model card (landscape variant) |
| MediaPipe Selfie Multiclass (256x256, float32) | Download | 16,371,837 | [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0) | [Multiclass segmentation model card](https://storage.googleapis.com/mediapipe-assets/Model%20Card%20Multiclass%20Segmentation.pdf) |
| MediaPipe Hair Segmenter (512x512, float32) | Download | 781,618 | [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0) | [Hair segmentation model card](https://storage.googleapis.com/mediapipe-assets/Model%20Card%20-%20Hair%20Segmentation.pdf) |
| MediaPipe DeepLab-V3 (257x257, float32) | Download | 2,780,176 | [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0) | [MediaPipe Image Segmenter task docs](https://ai.google.dev/edge/mediapipe/solutions/vision/image_segmenter) / [LiteRT community mirror](https://huggingface.co/litert-community/MediaPipe-ImageSegmenter-DeepLab-V3) |
| MODNet Portrait Matting (512x512, float32, LiteRT conversion) | Download | 26,166,776 | [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0) | [MODNet-LiteRT](https://huggingface.co/litert-community/MODNet-LiteRT) / [upstream MODNet](https://github.com/ZHKKKe/MODNet) |

### Attribution requested upstream

- **MediaPipe Selfie Segmentation** - model card by Tingbo Hou, Siargey Pisarchyk and Karthik Raveendran (Google),
  May 6, 2021; distributed by Google as part of MediaPipe under Apache-2.0.
- **MediaPipe Multiclass Segmentation** - model card by Adel Ahmadyan (Google), May 10, 2023; Apache-2.0.
- **MediaPipe Hair Segmentation** - developed by researchers at Google, 2019 (2019-01-14); Apache-2.0. The model
  card asks that work be cited as A. Tkachenka et al., "Real-time Hair segmentation and recoloring on Mobile
  GPUs", CVPR Workshop on Computer Vision for Augmented and Virtual Reality, 2019.
- **MediaPipe DeepLab-V3** - semantic segmentation model distributed by Google as part of MediaPipe; Apache-2.0.
- **MODNet** - "MODNet: Real-Time Trimap-Free Portrait Matting via Objective Decomposition", Zhanghan Ke,
  Jiayu Sun, Kaican Li, Qiong Yan, Rynson W.H. Lau (AAAI 2022); Apache-2.0. The app uses the LiteRT conversion
  published by the LiteRT community.

"MediaPipe" and "Google" are trademarks of Google LLC; "Hugging Face" is a trademark of Hugging Face, Inc.;
"MODNet" identifies the model of its original authors. This file does not grant trademark rights, and the app does
not imply endorsement by Google, Hugging Face or the MODNet authors.

### Where the licence evidence is

- [docs/product/MODEL_LICENSES.md](docs/product/MODEL_LICENSES.md) - per-model licence, source, fetch date, size,
  SHA-256 and intended use.
- [docs/product/licenses/](docs/product/licenses/) - the licence texts and model-card notices collected for the
  MediaPipe models and MODNet, plus the per-model licence evidence notes.
- [app/src/main/assets/models/NOTICE.txt](app/src/main/assets/models/NOTICE.txt) - the notice bundled in the app.
- [app/src/main/assets/models/LICENSE-Apache-2.0.txt](app/src/main/assets/models/LICENSE-Apache-2.0.txt) - the
  Apache-2.0 text bundled in the app.
- [app/src/main/assets/models/LiteRT-LICENSE.txt](app/src/main/assets/models/LiteRT-LICENSE.txt) and
  [app/src/main/assets/models/LiteRT-THIRD_PARTY_NOTICE.txt](app/src/main/assets/models/LiteRT-THIRD_PARTY_NOTICE.txt)
  - the LiteRT runtime licence and its third-party notice, bundled in the 0.2.3 build.
- [docs/product/model_manifest.json](docs/product/model_manifest.json) - the machine-readable manifest used by the
  downloader, with the pinned URL, byte size and SHA-256 for each model.

The same notices are reachable on the device from **Settings -> Licences** in the app. That screen renders the
bundled licence texts; it is not a substitute for this file or for the upstream licence texts.

## Development and test-only components (not distributed)

These are used to build or test the app and are not part of the APK/AAB. Their licences still apply to use of
their software:

| Component | Version | Licence | Use |
|---|---|---|---|
| Android Gradle Plugin | 8.10.1 | [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0) | Build |
| Kotlin Gradle Plugin / Compose compiler plugin | 2.0.21 | [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0) | Build |
| Gradle wrapper | 8.11.1 | [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0) | Build |
| JUnit 4 | 4.13.2 | [Eclipse Public License 1.0](https://www.eclipse.org/legal/epl-v10.html) | Unit tests |
| Robolectric | 4.16.1 | [MIT](https://opensource.org/licenses/MIT) | JVM UI/unit tests |
| AndroidX Test (`core`, `runner`, `ext-junit`, `ui-test-junit4`, `ui-test-manifest`) | 1.6.1 / 1.5.0 / 1.1.5 / 1.7.6 | [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0) | Instrumented tests |
| Compose UI Tooling (`ui-tooling`, debug variant only) | 1.7.6 | [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0) | Debug previews |

## Questions or corrections

If you believe a component is missing from this file, or that a licence is stated incorrectly, open an issue in
[this repository](https://github.com/ahmadmhmdsy/midad-studio/issues). Please include the component, version and the
evidence (POM, NOTICE file or upstream licence page) so it can be checked and corrected. For a security issue in a
component, follow [SECURITY.md](SECURITY.md) instead.

This file is informational and does not replace any upstream licence text; the upstream licence governs.
