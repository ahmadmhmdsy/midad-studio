# Model catalogue — added cut-out models (0.2.3)

**Status:** researched and verified for the 0.2.3 release, 2026-11-22.
**Scope:** TFLite float32 image-segmentation/matting models that run on the app's existing
LiteRT 2.3.2 interpreter, with no new native dependency and no cloud inference.

The app ships six catalogue entries. All four original MediaPipe models remain; this milestone
adds **MediaPipe DeepLab-V3** and **MODNet portrait matting**. Five are the complete set of
MediaPipe Image Segmenter families documented by Google (selfie square, selfie landscape, hair,
selfie multiclass, DeepLab-V3); there is no sixth MediaPipe segmentation family suitable for
this task.

## 1. Added models

### MediaPipe DeepLab-V3 — general objects and scenery

| Field | Value |
|---|---|
| Catalogue id | `mediapipe-deeplab-v3-257-f32` |
| Download | https://storage.googleapis.com/mediapipe-models/image_segmenter/deeplab_v3/float32/1/deeplab_v3.tflite |
| Latest alias | `.../deeplab_v3/float32/latest/deeplab_v3.tflite` (same bytes at verification) |
| Size / SHA-256 | 2,780,176 B · `ff36e24d40547fe9e645e2f4e8745d1876d6e38b332d39a82f0bf0f5d1d561b3` |
| Tensor spec | input `[1,257,257,3]` NHWC float32, RGB, `(x-127.5)/127.5`; output `[1,257,257,21]` float32 logits |
| Labels | PASCAL VOC: background, aeroplane, bicycle, bird, boat, bottle, bus, car, cat, chair, cow, dining table, dog, horse, motorbike, person, potted plant, sheep, sofa, train, tv |
| Licence | Apache-2.0 (MediaPipe / Google; HF mirror `litert-community/MediaPipe-ImageSegmenter-DeepLab-V3`) |
| App alpha | softmax over 21 channels, then `1 - P(background)` (sum of all foreground classes) |
| Runtime evidence | downloaded and installed on emulator-5564; inference completed through the app's TFLite path; XNNPACK replaced all 70 graph nodes |
| Limitation | not a portrait matte; a single foreground/background split cannot choose *which* object when several are present |

### MODNet portrait matting — soft hair and edges

| Field | Value |
|---|---|
| Catalogue id | `modnet-portrait-matting-512-f32` |
| Download | https://huggingface.co/litert-community/MODNet-LiteRT/resolve/main/modnet.tflite |
| Size / SHA-256 | 26,166,776 B · `f7d1e9eca9562abddb60e4b0a3ed0c29441ef09cd621954978f1613914cc64bc` |
| Tensor spec | input `[1,3,512,512]` **NCHW** float32, RGB, `(x/255-0.5)/0.5`; output `[1,1,512,512]` **NCHW** float32 alpha |
| Activation | none in the app: the graph ends in LOGISTIC, so the raw output is already alpha in [0,1] |
| Licence | Apache-2.0 (upstream `ZHKKKe/MODNet`); HF conversion `litert-community/MODNet-LiteRT` |
| App support added | four-dimensional tensors are now read with their declared layout; NCHW input is written plane-by-plane and a multi-channel NCHW output is transposed to pixel-major before mask maths. Single-channel planar output is already row-major and needs no transposition. |
| Runtime evidence | downloaded (26 MB) and installed on emulator-5564; on a 640 px portrait the cut-out kept the face, hair and shoulders with soft hair edges, and the transparent backdrop showed the app checkerboard |
| Cost | upstream benchmark ~419 ms CPU (4 threads) / ~79 ms GPU per 512×512 frame, plus first-load time; 25 MB download |
| Limitation | portraits only, not general objects; soft alpha may need feathering on low-contrast hair |

## 2. Why these two

- **DeepLab-V3** is the only additional MediaPipe segmenter family. It adds object/pet/product
  cut-outs that the portrait-only models cannot isolate, at 2.8 MB and ~124 ms CPU, and it
  already fits the NHWC float32 contract.
- **MODNet** is the strongest practical soft-matte upgrade. Its 26 MB download is large but
  bounded, it is Apache-2.0, and a 512×512 NCHW model is a small, testable addition to the
  existing interpreter. The result is visibly better around hair than a binary mask.

## 3. Evaluated and deferred

| Candidate | Evidence | Decision |
|---|---|---|
| litert-community/U-2-Net | 88,230,272 B, SHA-256 `dd338f190a538ca3de9792b20b7617038cd56f8e440f38ff25da5554f32b9df2`, Apache-2.0, NCHW `[1,3,320,320]`, needs per-image max normalization | Deferred: 88 MB, extra preprocessing mode, ~1.8 s CPU per frame |
| litert-community/BiRefNet-Lite | 184,001,424 B, MIT, NCHW likely | Deferred: over the comfortable download budget; tensor specs not execution-verified |
| litert-community/ormbg-LiteRT | 176,240,148 B, Apache-2.0 | Deferred: size |
| litert-community/DIS-ISNet-LiteRT | 176,223,448 B, Apache-2.0 | Deferred: size |
| litert-community/Cloth-Segmentation-U2Net | 176,300,680 B, MIT | Not a subject cut-out (clothing only) |
| U2Net portrait sketch | 176,124,376 B, Apache-2.0 | Output is a sketch, not a matte |
| lumen-cutout int8 models | 63–134 MB, MIT | App is float32-only; int8 is not supported |
| `deshanc/unet-mobilenet-portrait-segmentation` | 3,939,176 B, MIT | Not inspected; likely no clear gain over the MediaPipe portrait models |

## 4. Must not ship

| Candidate | Reason |
|---|---|
| PeterL1n/RobustVideoMatting | GPL-3.0 |
| imgly/background-removal-js | AGPL-3.0 |
| briaai/RMBG-1.4 | source-available, non-commercial licence |
| rembg / onnx-community ISNet | ONNX format; app uses TFLite |
| PINTO_model_zoo or random GitHub ports | no verified licence/provenance |

## 5. Recheck commands

Every size and hash above was obtained with HTTP HEAD/GET plus `Get-FileHash`; tensor specs
were read from the `.tflite` flatbuffer. The manifest records the same values and the
verification method. To recheck a model:

```powershell
$url = 'https://storage.googleapis.com/mediapipe-models/image_segmenter/deeplab_v3/float32/1/deeplab_v3.tflite'
Invoke-WebRequest -Method Head $url | Select-Object StatusCode, Headers
Invoke-WebRequest -Uri $url -OutFile "$env:TEMP\deeplab_v3.tflite"
(Get-FileHash "$env:TEMP\deeplab_v3.tflite" -Algorithm SHA256).Hash
```

The catalogue is data, not code: adding a compatible float32 model that follows the existing
input/output contract does not require a new release of the interpreter. A model that needs a
new normalization, layout or activation must extend `TfliteSegmenter` and its tests first.
