# MODNet (`modnet.tflite`, LiteRT conversion) — model-level licence evidence

**Recorded:** 2026-11-22 for **release 0.2.3**.
**Licence: Apache-2.0 — shipped as a runtime download.** Summarised in [MODEL_LICENSES.md](../MODEL_LICENSES.md) §1 and §2.2.
**Companion files:** [modnet-LICENSE.txt](./modnet-LICENSE.txt) (upstream Apache-2.0 text, byte-identical to the file served from GitHub) and [modnet-model-card-NOTICE.txt](./modnet-model-card-NOTICE.txt) (attribution to reproduce if we ever re-host the bytes).
**Not a legal opinion.** This file records evidence and dates; it is not legal advice.

## 1. The artifact the app downloads

| Field | Value |
|---|---|
| Catalogue id | `modnet-portrait-matting-512-f32` (app `manifest.json`, 0.2.3) |
| File | `modnet.tflite` — LiteRT/TFLite float32, input `[1,3,512,512]` NCHW RGB, output `[1,1,512,512]` soft alpha |
| Distribution | **DOWNLOAD only** — fetched at runtime by the app; it is **not** in the APK/AAB and neither is the licence text beyond the shared Apache-2.0 copy |
| Download / source URL | https://huggingface.co/litert-community/MODNet-LiteRT/resolve/main/modnet.tflite |
| Size | **26,166,776 bytes** |
| SHA-256 (local, 2026-11-22) | `f7d1e9eca9562abddb60e4b0a3ed0c29441ef09cd621954978f1613914cc64bc` |
| Publisher record (Hugging Face) | repo `litert-community/MODNet-LiteRT`, commit `4d5bd22bde15c8ae5e215ca4799513844979a431`, `lastModified` 2026-09-08T08:52:54Z, `createdAt` 2026-07-06T12:40:53Z; LFS object sha256 = `f7d1e9ec…` and size 26,166,776 (match) |
| Upstream model | https://github.com/ZHKKKe/MODNet (default branch `master`; "A Trimap-Free Portrait Matting Solution in Real Time [AAAI 2022]") |
| Paper | Zhanghan Ke, Jiayu Sun, Kaican Li, Qiong Yan, Rynson W.H. Lau. "MODNet: Real-Time Trimap-Free Portrait Matting via Objective Decomposition." AAAI 2022. https://arxiv.org/abs/2011.11961 |

## 2. Licence evidence — two independent Apache-2.0 statements

### 2.1 Upstream repository (the model's authors) — Apache-2.0

| Field | Value |
|---|---|
| Repository | https://github.com/ZHKKKe/MODNet |
| GitHub licensing API | `license.spdx_id: "Apache-2.0"` (fetched 2026-11-22) |
| `LICENSE` file | path `LICENSE`, **11,357 bytes**, git blob SHA-1 `261eeb9e9f8b2b4b0d119366dda99c6fd7d35c64`; raw SHA-256 `c71d239df91726fc519c6eb72d318ec65820627232b2f796219e87dcf35d0ab4` (downloaded 2026-11-22; local copy in this directory matches) |
| Comparison with apache.org text | same Apache License 2.0 text; the canonical https://www.apache.org/licenses/LICENSE-2.0 file fetched on 2026-11-22 is 11,358 B, sha256 `cfc7749b96f63bd31c3c42b5c471bf756814053e847c10f3eb003417bc523d30`; the upstream `LICENSE` is that text without the leading blank line (verified byte-for-byte) |
| NOTICE file | **none** in the repository root (root listing 2026-11-22: `.gitignore`, `LICENSE`, `README.md`, `demo`, `doc`, `onnx`, `pretrained`, `src`, `torchscript`) |
| Copyright line in LICENSE | none — the Apache-2.0 text carries no per-file copyright line; the authors and the paper are the attribution recorded in our NOTICE and licence screen |

### 2.2 Hugging Face model card (the LiteRT conversion's publisher) — Apache-2.0

| Field | Value |
|---|---|
| Model card | https://huggingface.co/litert-community/MODNet-LiteRT (README = model card, front matter) |
| Licence metadata | front matter `license: apache-2.0`; card data `{"license":"apache-2.0","library_name":"litert","pipeline_tag":"image-segmentation"}`; repo tag `license:apache-2.0` (API fetched 2026-11-22) |
| Weights attribution on the card | "**Weights:** [ZHKKKe/MODNet](https://github.com/ZHKKKe/MODNet) · Apache-2.0 · ~6.5 M params"; "**Size:** 26 MB" |
| Conversion statement on the card | converted with **litert-torch** (`build_modnet.py`), applying two re-authoring patches for GPU compatibility: **SE block `Linear` → 1×1 conv** and **fp16-safe hierarchical-mean `InstanceNorm`**; CPU-exact vs PyTorch (corr 0.99999999999), device Mali GPU corr 0.99994 |
| I/O on the card | input `[1, 3, 512, 512]` NCHW RGB `(x/255-0.5)/0.5` → `[-1,1]`; output `[1, 1, 512, 512]` alpha in `[0,1]` — matches the app manifest |
| Note | the HF repo does **not** ship a `LICENSE` file (siblings: `.gitattributes`, `README.md`, `hero.png`, `modnet.tflite`); its licence statement is the card metadata. The licence text we rely on is the upstream repository `LICENSE` saved here. |

## 3. Apache-2.0 obligations and how 0.2.3 meets them

| Section | Obligation | Where met in 0.2.3 |
|---|---|---|
| §4(a) | Give recipients a copy of the licence | The app ships the full Apache-2.0 text at `app/src/main/assets/models/LICENSE-Apache-2.0.txt` (12,331 B, sha256 `8707eef0…`; MediaPipe's copy = Apache-2.0 text + Lucent appendix) and the licence screen names MODNet + Apache-2.0 |
| §4(b) | Cause modified files to carry prominent notices stating that they were changed | The LiteRT conversion is a modified/derived model produced by **litert-community**, not by Midad Studio; the app downloads the published file **unmodified**. We do not redistribute it. *Recommended:* state the conversion modifications in the shipped NOTICE for transparency, and carry the modification statement in `modnet-model-card-NOTICE.txt` if the bytes are ever re-hosted |
| §4(c) | Retain all copyright, patent, trademark and attribution notices | The upstream LICENSE carries no per-file copyright line; the authors are credited by name in the shipped NOTICE and in the in-app licence screen |
| §4(d) | If the work includes a NOTICE file, reproduce its attribution notices | Neither the upstream repo nor the HF repo has a NOTICE file → nothing further to reproduce. Our own attribution statement is additional, not a substitute obligation |
| §6 | No trademark licence | "MODNet" identifies the authors' model; it is not used in the app name, icon or listing; no endorsement by the authors is implied. The app NOTICE says so explicitly |

## 4. Residual notes / UNRESOLVED (not distribution blockers)

1. **Training-data provenance (UNRESOLVED).** The weights were released by the authors under Apache-2.0 (repository declaration + HF card), which is the distribution licence. What corpus the authors trained on was **not** re-verified for 0.2.3; earlier dataset work records P3M-10k's repository as MIT, but that record was not confirmed as MODNet's training set. Ask the authors or counsel for data-level comfort before any claim that the whole weight lineage is provenanced.
2. **Operator of `litert-community` (UNRESOLVED).** The Hugging Face organisation's ownership/affiliation is not independently evidenced here. Reliance is on the conversion's declared licence and on the upstream authors' Apache-2.0 repository — **not** on an implied Google/Hugging Face endorsement. No endorsement is claimed anywhere in the app.
3. **Persistence.** The HF repo is the same one the app downloads from; if the card metadata or the file ever changes, treat the new bytes as unreviewed and re-run §5 before shipping.

## 5. Reproduction (2026-11-22)

```powershell
# artifact
curl.exe -sS -L -o modnet.tflite "https://huggingface.co/litert-community/MODNet-LiteRT/resolve/main/modnet.tflite"
(Get-FileHash -Algorithm SHA256 .modnet.tflite).Hash.ToLower()
# expect f7d1e9eca9562abddb60e4b0a3ed0c29441ef09cd621954978f1613914cc64bc

# upstream licence
curl.exe -sS -L -o modnet-LICENSE.txt "https://raw.githubusercontent.com/ZHKKKe/MODNet/master/LICENSE"
(Get-FileHash -Algorithm SHA256 .modnet-LICENSE.txt).Hash.ToLower()
# expect c71d239df91726fc519c6eb72d318ec65820627232b2f796219e87dcf35d0ab4

# licence metadata
curl.exe -sS -L "https://api.github.com/repos/ZHKKKe/MODNet" | Select-String "spdx_id"
curl.exe -sS -L "https://huggingface.co/api/models/litert-community/MODNet-LiteRT" | Select-String "apache-2.0"
```

## 6. Source index

| Source | URL | Fetched | What it establishes |
|---|---|---|---|
| Upstream repository + license API | https://github.com/ZHKKKe/MODNet · https://api.github.com/repos/ZHKKKe/MODNet | 2026-11-22 | `spdx_id: Apache-2.0`; repo identity, description, default branch |
| Upstream `LICENSE` (raw) | https://raw.githubusercontent.com/ZHKKKe/MODNet/master/LICENSE | 2026-11-22 | Apache-2.0 text, 11,357 B, sha256 `c71d239d…`; no NOTICE file in root |
| HF model card + API | https://huggingface.co/litert-community/MODNet-LiteRT · https://huggingface.co/api/models/litert-community/MODNet-LiteRT | 2026-11-22 | `license: apache-2.0`; weights attributed to ZHKKKe/MODNet Apache-2.0; conversion method and patches; commit `4d5bd22b…`; LFS sha256 matches artifact |
| Paper | https://arxiv.org/abs/2011.11961 | 2026-11-22 (identifier only; paper not re-read in full) | Citation: Ke, Sun, Li, Yan, Lau — AAAI 2022 |
| Apache-2.0 canonical text | https://www.apache.org/licenses/LICENSE-2.0 | 2026-11-22 | Text comparison (11,358 B, sha256 `cfc7749b…`) |

*Recorded for Midad Studio 0.2.3. The model ships only as a runtime download; the app verifies its hash before use. Not a legal opinion.*
