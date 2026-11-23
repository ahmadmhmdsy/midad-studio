# MediaPipe DeepLab-V3 (`deeplab_v3.tflite`) — model-level licence evidence

**Recorded:** 2026-11-16; updated 2026-11-22 for **release 0.2.3**.
**Licence: Apache-2.0.** The model is **not bundled** in the app (product choice, not a licence restriction). **In 0.2.3 it ships as a runtime download** — catalogue id `mediapipe-deeplab-v3-257-f32`, `distribution: "DOWNLOAD"` — see §6.

> **Update 2026-11-22 (0.2.3):** shipped as a **runtime download** (catalogue id `mediapipe-deeplab-v3-257-f32`, not in the APK) after the general-object feature was added. The licence status is unchanged; the Hugging Face mirror card adds model-card-level Apache-2.0 evidence. See **§6**.

## 1. The artifact in question

| Field | Value |
|---|---|
| File | `deeplab_v3.tflite` (MediaPipe Image Segmenter, DeepLab-V3, 257x257, float32) |
| Pinned URL | https://storage.googleapis.com/mediapipe-models/image_segmenter/deeplab_v3/float32/1/deeplab_v3.tflite |
| `latest` alias | https://storage.googleapis.com/mediapipe-models/image_segmenter/deeplab_v3/float32/latest/deeplab_v3.tflite (byte-identical: same size + GCS ETag) |
| Size | 2,780,176 bytes |
| SHA-256 | `ff36e24d40547fe9e645e2f4e8745d1876d6e38b332d39a82f0bf0f5d1d561b3` (computed locally from the pinned URL, 2026-11-16) |
| GCS ETag / Last-Modified | `89cbf585278bae20ce7606c07d7071dd` / 2023-04-26 03:33:40 GMT |
| Provenance stated by MediaPipe docs | links the file; the corresponding Kaggle Models page describes the model as *"TF Lite deployment of tensorflow/deeplabv3/1"* |

Model licence recorded: **Apache-2.0** (see §2) · Apache-2.0 text: https://www.apache.org/licenses/LICENSE-2.0

## 2. Model-level licence evidence

The licence is **not** on the human-readable Kaggle model page text (no licence line in the page body), but Kaggle's **public Models API** exposes a `licenseName` per model instance. Query used (anonymous, HTTP 200, 152,613 bytes, fetched 2026-11-16 with `curl.exe -sS -L`):

```
GET https://www.kaggle.com/api/v1/models/list?pageSize=20&search=deeplab
```

Record for `https://www.kaggle.com/models/tensorflow/deeplabv3` (author **TensorFlow**, published 2020-10-06, updated 2023-02-15):

```json
{
  "url": "https://www.kaggle.com/models/tensorflow/deeplabv3",
  "author": "TensorFlow",
  "publishTime": "2020-10-06T00:00:00Z",
  "updateTime": "2023-02-15T19:31:53.8187496Z",
  "provenanceSources": "https://github.com/tensorflow/tfhub.dev/tree/master/assets/docs/tensorflow",
  "instances": [
    {
      "slug": "default",
      "framework": "tfLite",
      "versionNumber": 1,
      "licenseName": "Apache 2.0",
      "totalUncompressedBytes": 2779264,
      "downloadUrl": "/models/tensorflow/deeplabv3/TfLite/default/1/download"
    },
    {
      "slug": "metadata",
      "framework": "tfLite",
      "versionNumber": 2,
      "licenseName": "Apache 2.0",
      "totalUncompressedBytes": 2780051,
      "downloadUrl": "/models/tensorflow/deeplabv3/TfLite/metadata/2/download"
    }
  ]
}
```

- **Licence name:** Apache 2.0 (SPDX: `Apache-2.0`) on **both** TF Lite instances (`default` v1 and `metadata` v2), including the v1 instance that the MediaPipe model is derived from.
- **Licence URL:** https://www.apache.org/licenses/LICENSE-2.0
- **Licence evidence URL:** https://www.kaggle.com/api/v1/models/list?pageSize=20&search=deeplab (JSON API, fetched **2026-11-16**); human page https://www.kaggle.com/models/tensorflow/deeplabv3
- **Author / provenance:** TensorFlow (Kaggle org), provenance source tfhub.dev assets docs.

## 3. Negative evidence

1. **No DeepLab-V3 model card exists in Google's public MediaPipe assets bucket.** Full enumeration of `https://storage.googleapis.com/mediapipe-assets` (XML listing, paginated, **5,956 keys**; fetched 2026-11-16) found **13 model-card PDFs — none for DeepLab-V3** (cards exist for Selfie Segmentation, Multiclass Segmentation, Hair Segmentation, BlazeFace, BlazePose, Blendshape, Hand Tracking, Face Mesh, Face Stylizer, MagicTouch, LanguageDetector).
2. **Embedded TFLite metadata `license` field is empty** in the file (parsed from the flatbuffer 2026-11-16 — same as every other MediaPipe model).
3. **DeepLab README only licenses the code** (`research/deeplab`, Apache-2.0) — that alone does not license the weights.

So: no MediaPipe model card, but the upstream model's own publisher (TensorFlow, Kaggle Models) records the TF Lite deployment as **Apache 2.0** at model level. That is the model-level licence record. It was independently re-checked on 2026-11-16 by re-fetching the API and re-downloading the artifact. Caveats: (i) the evidence is a machine-readable API field, not a rendered licence panel and not a MediaPipe model card (none exists); (ii) the MediaPipe build is **not byte-identical** to either Kaggle instance (125 B difference vs the metadata v2 instance, 2,780,051 B), so it is a modified redistribution — Apache-2.0 permits that; (iii) if bundled, ship the Apache-2.0 text, a NOTICE naming Google/MediaPipe/TensorFlow, and a manifest row.

## 4. Bundling package (ready if the model is ever bundled)

- [`mediapipe-deeplab-v3-LICENSE.txt`](./mediapipe-deeplab-v3-LICENSE.txt) — verbatim Apache License, Version 2.0 (same 12,331-byte text as the other MediaPipe models, SHA-256 `8707eef0533987efc5b155d64761eeb6e20793f50b9bd1a68dad1cf4719d0ed8`).
- NOTICE addition required when bundled: name *TensorFlow DeepLab-V3 / MediaPipe* (Google LLC) and cite the Kaggle Models page plus this API licence record (no MediaPipe model card exists); state that the MediaPipe build is a modified distribution (not a byte-match to the Kaggle instances, 125 B difference vs v2), which Apache-2.0 permits.
- App integration (**done in 0.2.3, 2026-11-22**): the model is fetched at runtime (not copied into `app/src/main/assets/models/`), the app manifest entry is in place, and the licence record above (origin Kaggle Models `tensorflow/deeplabv3` (TF Lite), author TensorFlow/Google, Apache-2.0, SHA-256 above) is what the attribution records. See §6 for the 0.2.3 record.

## 5. Verification reproduction

```powershell
curl.exe -sS -L -o deeplab_v3.tflite https://storage.googleapis.com/mediapipe-models/image_segmenter/deeplab_v3/float32/1/deeplab_v3.tflite
(Get-FileHash -Algorithm SHA256 .\deeplab_v3.tflite).Hash.ToLower()
# expect ff36e24d40547fe9e645e2f4e8745d1876d6e38b332d39a82f0bf0f5d1d561b3
curl.exe -sS -L "https://www.kaggle.com/api/v1/models/list?pageSize=20&search=deeplab" -o kaggle_models_deeplab.json
# inspect licenseName for url == https://www.kaggle.com/models/tensorflow/deeplabv3 (expect "Apache 2.0")
```

## 6. 0.2.3 addendum — shipped as a runtime download; Hugging Face mirror verified (2026-11-22)

**Recorded 2026-11-22.** Nothing in §1–§5 is withdrawn; this section records the product decision and one new piece of model-card-level evidence.

- **Product status changed, licence status did not.** Release **0.2.3** (versionCode 5) ships DeepLab-V3 as a **runtime download**, catalogue id `mediapipe-deeplab-v3-257-f32`: app manifest `distribution: "DOWNLOAD"`, `sourceUrl`/`downloadUrl` = the pinned MediaPipe GCS URL in §1, size/SHA-256 exactly as recorded there (2,780,176 B, `ff36e24d…`), `verification.verifiedAt: 2026-11-22`. It is **not** bundled in the APK/AAB. The "not shipped in v1 / entry stays disabled" wording in earlier revisions of this file is superseded by 0.2.3; the licence record was not reopened.
- **New model-card-level evidence: the official LiteRT mirror.** https://huggingface.co/litert-community/MediaPipe-ImageSegmenter-DeepLab-V3 (model card, API fetched 2026-11-22):
  - front-matter and card data: `license: apache-2.0`; repo tag `license:apache-2.0`;
  - repo commit `739202fd92e4a822ba439afe91bdf69254227d94`, `lastModified` 2026-11-19T20:33:59Z, `createdAt` 2026-11-18T23:07:27Z;
  - file sibling `deeplab_v3.tflite`: size **2,780,176**, LFS sha256 `ff36e24d40547fe9e645e2f4e8745d1876d6e38b332d39a82f0bf0f5d1d561b3` — **identical to the bytes the app downloads from the GCS URL** (independently re-downloaded and re-hashed 2026-11-22);
  - the card describes the same artifact: 257x257 FP32, 21 PASCAL VOC classes, `[1,257,257,3]` input, `[1,257,257,21]` output, embedded `TFLITE_METADATA` author `MediaPipe`.
  This gives the model a model-card-level Apache-2.0 statement at the mirror, in addition to the Kaggle publisher API field (§2) — while the negative evidence in §3 (no MediaPipe model card in the mediapipe-assets bucket) remains true.
- **Kaggle caveat retained.** The MediaPipe-hosted build (2,780,176 B) is still **not byte-identical** to either Kaggle TF Lite instance (default v1 2,779,264 B; metadata v2 2,780,051 B): it is a modified distribution by Google, permitted by Apache-2.0. We redistribute it unmodified (runtime download from Google/HF hosts), so no §4(b) notice duty falls on Midad Studio; the app does not state that its bytes equal the Kaggle build.
- **Attribution in 0.2.3.** `app/src/main/assets/models/NOTICE.txt` §5 names the model, both source hosts and the Kaggle record, and cites Chen et al. (IEEE TPAMI 2018); the in-app licence screen has a MediaPipe DeepLab-V3 entry. The final 18:34Z build embeds the current 6,512 B NOTICE and the DeepLab-V3 manifest row (see [MODEL_LICENSES.md](../MODEL_LICENSES.md) §5).

### Reproduction

```powershell
curl.exe -sS -L -o deeplab_v3.tflite "https://storage.googleapis.com/mediapipe-models/image_segmenter/deeplab_v3/float32/1/deeplab_v3.tflite"
(Get-FileHash -Algorithm SHA256 .\deeplab_v3.tflite).Hash.ToLower()
# expect ff36e24d40547fe9e645e2f4e8745d1876d6e38b332d39a82f0bf0f5d1d561b3
curl.exe -sS -L "https://huggingface.co/api/models/litert-community/MediaPipe-ImageSegmenter-DeepLab-V3"
# inspect cardData.license (apache-2.0), sha (739202fd...), siblings[deeplab_v3.tflite].lfs.sha256
```

*Addendum written 2026-11-22 for release 0.2.3. Not a legal opinion.*

*Recorded for Midad Studio on 2026-11-16; re-verified on 2026-11-22. Not a legal opinion.*
