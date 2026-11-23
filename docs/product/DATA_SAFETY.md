# Data safety — Midad Studio's data handling and Google Play Data safety answers

**App:** Midad Studio · `com.midad.studio` · **versionName 0.2.3** · **versionCode 5**
**Prepared:** 2026-11-17 · **Re-checked:** 2026-11-21 against the 0.2.2 build and again on 2026-11-22 against the
0.2.3 build — **no answer changed**: projects and their thumbnails are app-private local storage, not collection or
sharing; 0.2.3 adds no permission, SDK, analytics or new endpoint class. The new user-visible work is local
playback (exact video preview), reading the app's own exported file (result dialog) and one more **user-initiated
model file** download (MODNet, from `huggingface.co`; the other five models stay on
`storage.googleapis.com`). Neither preview nor the result dialog transmits anything.

This page maps each question in Google Play's **Data safety** form to the app's actual behaviour, with the
evidence that can be checked against the shipped build. If the app and this page disagree, the app is right and
this page must be fixed. Midad Studio is not currently distributed through Google Play; the answers below follow
from the app's behaviour and can be audited against any build.

> **Hard precondition.** Every answer below assumes the distributed artifact's manifest declares **one user-facing
> permission**, `android.permission.INTERNET`, plus AndroidX's own signature-level
> `com.midad.studio.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`, and that `android:allowBackup="false"` with
> data-extraction rules excluding all domains is present. Verify on the **exact file you distribute**, not on the
> source tree:
>
> ```powershell
> # if you build an APK locally for the check
> & "$env:ANDROID_HOME\build-tools\36.0.0\aapt2.exe" dump permissions app-release.apk
> # for an AAB (bundletool)
> java -jar bundletool.jar dump manifest --bundle app-release.aab | Select-String uses-permission
> ```
>
> An AAB cannot be permission-dumped with `aapt2` directly; use `bundletool dump manifest` or install the
> generated APKs and dump those. If a `READ_MEDIA_*`, `READ_EXTERNAL_STORAGE` or `WRITE_EXTERNAL_STORAGE`
> entry appears, **stop**: the merged manifest must be fixed first, because this form would then be false.

> **What was verified (2026-11-17 18:27 final build).** The signed release APK and the signed AAB were built from
> the current tree. `aapt2 dump permissions` on both the release and debug APKs lists only `INTERNET` and
> `com.midad.studio.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`; the merged release manifest and the **AAB manifest
> itself** (dumped with `bundletool 1.18.1`) show `allowBackup="false"`, `targetSdk 36`, minSdk 26 and the same
> two permissions; `bundletool validate` exits 0. Hashes and checks:
> [../qa/BUILD_REPORT.md](../qa/BUILD_REPORT.md) §2–§4.

---

## 0. How Play defines the words (this decides every answer)

| Term | Play's meaning | Consequence here |
|---|---|---|
| **Collect** | Transmit data off the user's device — to you, to a third party, or to a service you use | On-device editing and on-device storage are **not** collection |
| **Share** | Transfer data to a third party, even a processor acting for you | Nothing is transferred to anyone for app purposes |
| **Required data type** | The 14 categories the form enumerates (location, personal info, financial, health, messages, photos, audio, files, calendar, contacts, app activity, web browsing, app info and performance, device IDs) | All 14 are answered "not collected" in §2 |
| **Processing** | Includes on-device use of data | Local use is still not collection until it leaves the device |

The only outbound request the app makes is a **model-file download that the user starts**. It carries no user
data: no identifier, no photo, no filename, no usage event. The host sees ordinary connection metadata (IP
address, user agent, time), exactly as any HTTPS download reveals it. That is not data collected *by us*: we
operate no server and receive no record.

---

## 1. Top-level answers

| # | Form question | Answer | Why |
|---|---|---|---|
| 1 | Does your app collect or share any of the required user data types? | **No** | No server, no account, no SDK that transmits data. Media, edits, exports, playback and settings stay on the device. The only outbound requests are user-initiated model-file downloads, two hosts, carrying no user data. |
| 2 | (If yes) which data types, and for what purpose? | **Not applicable** | Skip it. Do not tick purposes "for completeness" — that is how a "no collection" app fails review. |
| 3 | Is all user data encrypted in transit? | **Not applicable — no data is collected.** Do not answer "yes" to a question about data that does not exist. | Model files are fetched over HTTPS with SHA-256 verification. |
| 4 | Do you provide a way for users to request deletion of their data? | **Not applicable — no server-side data exists.** Local deletion is real and immediate: delete a project, delete a model, uninstall. | [PRIVACY_POLICY.md](PRIVACY_POLICY.md) §5 and §9 |
| 5 | Has your app been independently validated against a global security standard (e.g. MASA)? | **No** | No audit has been performed. Do not claim one. |
| 6 | Does your app contain ads? | **No** | No ad SDK, no advertising ID, no interstitial or rewarded surface. |
| 7 | Is your app designed for children / families? | **No — target audience 13+** | No accounts, no ads, no purchases, no social features; not marketed to children. |
| 8 | Does the app let users create accounts? | **No** | No sign-in, no profile, no backend. |
| 9 | Does the app request the Photo and Video Permissions? | **No** | It uses `PickVisualMedia` / `PickMultipleVisualMedia`; the source manifest requests no `READ_MEDIA_*` or storage permission. Re-verify on the artifact. |

---

## 2. The 14 data types, one by one

For each category the form asks: collected? shared? optional or required? purposes? Every category is
**not collected and not shared**, for the same structural reason: the app has no transmission path for it.

| # | Play data type | Collected | Shared | Why the answer is "no" |
|---|---|---|---|---|
| 1 | **Location** (approximate, precise) | No | No | No location permission and no location API call in the app. |
| 2 | **Personal info** (name, email, address, phone, race/ethnicity, political or religious beliefs, sexual orientation, other) | No | No | No account, no form, no profile, no contact import. |
| 3 | **Financial info** (payment info, purchase history, credit score, other) | No | No | No billing code; the app is free; no in-app purchases. |
| 4 | **Health and fitness** | No | No | No health APIs, no sensors, no health data requested. |
| 5 | **Messages** (email, SMS, in-app messages, other) | No | No | No messaging feature; no SMS or email access. |
| 6 | **Photos and videos** | No | No | Items are opened through the Photo Picker, edited **in memory**, and written back to the user's own gallery through MediaStore. Nothing is transmitted. The exact video preview decodes locally in a Media3 player and the export result dialog reads the file the app just wrote; neither sends anything. A thumbnail of a recent export is stored in app-private storage excluded from backup and is never uploaded. |
| 7 | **Audio** (voice recordings, music, other) | No | No | No microphone permission and no audio import; a video's own audio track is carried through the Media3 export and is never transmitted. |
| 8 | **Files and docs** | No | No | No document picker, no file upload, no cloud file access. |
| 9 | **Calendar** | No | No | No calendar permission or API. |
| 10 | **Contacts** | No | No | No contacts permission or API. |
| 11 | **App activity** (interactions, in-app search history, installed apps, other user-generated content, other actions) | No | No | No analytics SDK, no event log, no search history, no reading of installed apps. UI state stays in memory and process-private storage. |
| 12 | **Web browsing** | No | No | No browser, no WebView, no URL history. |
| 13 | **App info and performance** (crash logs, diagnostics, other) | No | No | No crash SDK, no diagnostics upload. The OS may write a local log; it is not collected by us and not readable to us. |
| 14 | **Device or other IDs** | No | No | No advertising ID, no installation ID, no IMEI/serial/Android ID use, no fingerprinting. |

**Purposes section:** skipped, because nothing is collected. **Optional/required:** not applicable for the same
reason.

---

## 3. Security practices and declarations answered elsewhere

| Declaration | Answer | Evidence |
|---|---|---|
| Data encrypted in transit | N/A (nothing collected). The optional model download uses HTTPS. | `DefaultModelRepository.downloadRemote` accepts `https://` only and refuses redirects to non-HTTPS; SHA-256 verified before install |
| Data deletion request path | Not applicable (no server). In-app deletion exists. | Delete project/export; delete model; uninstall removes private storage |
| Committed to Play Families policy | Not applicable — not a families app | Target audience 13+ |
| Independent security review | No | — |
| Photo and Video Permissions declaration | **Not required** | Photo Picker + MediaStore; no `READ_MEDIA_*` or storage permission requested |
| Account deletion requirement | Not applicable | No accounts |
| Government app | No | — |
| News app | No | — |
| COVID-19 contact tracing | No | — |
| Financial features | No | — |
| Health features | No | — |
| Ads declaration | No ads | — |
| App access | All functionality available without login or special access | No account or gated content |

---

## 4. The one ambiguity — and how to answer it

**The ambiguity.** The app makes HTTPS requests to fetch optional AI model files (six-model catalogue; MediaPipe
files from `storage.googleapis.com`, the MODNet conversion from `huggingface.co`). A strict reader can ask:
"something leaves the device, so is that data collection?"

**The answer: no — it is app functionality, not data collection.** The reasons, in order of weight:

1. **The request's payload contains no user data.** It is a `GET` for a public model file with a fixed
   `User-Agent` and `Accept: application/octet-stream`. No photo, no project, no filename, no device
   identifier, no account token, no advertising ID, no event, no crash report. You could reconstruct the entire
   request payload from the app's public catalogue. The host differs per model (two hosts), but the request shape
   and the absence of user data are identical.
2. **Play's definition is about data *you* collect or share.** The host's ordinary connection metadata (IP,
   timestamp, bytes) is a property of any download on the internet, is not linked by us to a user, and we
   receive nothing back — no receipt, no list, no identifier.
3. **The user initiates it, and can decline forever.** Nothing downloads without a tap; the app is fully
   functional offline with the bundled model; the download can be cancelled and the file deleted.
4. **It is the app doing its job** — the same category as fetching a font or an OTA resource, not a telemetry
   channel. It is not analytics, not advertising, not a crash report, not a "personalised experience".

**How the answers map to Play's form:** "Does your app collect or share any of the required user data types?"
→ **No**. Do **not** declare "Device or other IDs" or "App activity" to be safe: a false positive is a policy
violation too, and it would contradict the privacy policy, the listing and the app's behaviour.

**Two hosts, same answer.** On 2026-11-22 the catalogue has six models: five are fetched from
`storage.googleapis.com` (MediaPipe) and the MODNet conversion from `huggingface.co`. A second host is not a second
kind of data flow — both receive only the request for a public file and its ordinary connection metadata, and the
app sends nothing about the user to either. The model `manifest.json` pins the MODNet source and download URLs to
a specific Hugging Face commit (`4d5bd22bde15c8ae5e215ca4799513844979a431`; a `main` alias is kept in the manifest
but is not the download source), and every model file is checked against its manifest SHA-256 before use. Policy
version 4.0 of the public privacy text ([PRIVACY_POLICY.md](PRIVACY_POLICY.md) §3 and
[../privacy/index.html](../privacy/index.html)) names both hosts, while the Data safety answer stays **No
collection**. Keep the policy text and the manifest's host list in sync in future releases.

**What would change this answer:** if a future version sent anything *about the user or the device* with that
request (an install ID, an event, a version-and-device header used for analytics), or if the model host started
logging requests in a way we could read and link to a user, then the answer becomes "yes" and the form, the
policy and the listing must be updated in the same change.

---

## 5. Why "no collection" is defensible — the edge cases answered

| Objection a reviewer could raise | Answer |
|---|---|
| "The app makes an HTTPS request, so something is transmitted." | It fetches a public model file and carries no user data. Standard connection metadata is visible to the host as with any download; we receive nothing. See §4. |
| "Thumbnails of the user's photos are stored." | Local, app-private, excluded from backup, never transmitted. Local storage is not collection. |
| "The AI cut-out processes faces." | Processing happens in device memory and is discarded; nothing is sent anywhere and no model training occurs. |
| "Google Play itself collects crash data." | Play vitals are collected by Google as the distribution platform under its own policy, not by the app. The app ships no crash SDK. |
| "The app reads the gallery." | It does not browse the gallery. The Photo Picker returns only the items the user selected. |
| "The merged manifest still had storage permissions when I looked." | A build packaged before the final manifest edit still contained them. Verify the artifact you distribute: the source manifest removes them and the post-edit merge contains `INTERNET` only. If the artifact still shows them, do not submit. |
| "There is a second permission in the manifest." | `com.midad.studio.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` is an AndroidX signature-level permission the app declares for itself so its dynamic receivers are not exported. It is not a runtime permission, is not grantable to other apps, and exposes no user data. It is not a data-collection permission and is not answered anywhere on this form. |

---

## 6. What would make these answers false

Any one of these requires the manifest, the privacy policy, the listing and this page to change **in the same
commit** — otherwise the release and this document contradict each other:

- Adding an account, cloud sync, or any backend call that carries user data.
- Adding an analytics, crash-reporting, attribution or ad SDK.
- Adding a permission (camera, microphone, location, contacts, `READ_MEDIA_*`, storage).
- Sending an install ID, device model or version header for analytics with the model request.
- A support form that collects device IDs or attachments.
- Re-enabling Android backup or device transfer for app data.

---

## 7. Evidence to record with a release

Record these with each release (none of them needs a third-party service):

| Evidence | How to produce | Record |
|---|---|---|
| Permission dump of the distributed artifact | `aapt2 dump permissions` on a locally built APK, or `bundletool dump manifest --bundle` for the AAB | Text file, dated |
| Merged manifest | `app/build/intermediates/merged_manifests/release/processReleaseManifest/AndroidManifest.xml` **from the release build that produced the distributed AAB** | Copy |
| Dependency list grepped for tracking SDKs | `:app:dependencies --configuration releaseRuntimeClasspath`, then search for `ads`, `analytics`, `firebase`, `crashlytics`, `sentry`, `appsflyer`, `adjust`, `facebook` | Text file |
| Network-call inventory | Grep app sources for `http`, `URL(`, `OkHttp`, `HttpURLConnection` | Text file |
| Privacy policy URL + archived copy | https://ahmadmhmdsy.github.io/midad-studio/privacy/ and a dated PDF/HTML snapshot | Link + file |
| This document | This page, with its revision date | PDF/text |

---

## 8. Verification record

> These checks were run against the **2026-11-17 18:27 build** and repeated on the 0.2.3 build. Re-verify them
> on the exact artifact you distribute, and again after any rebuild.

| Check | Result |
|---|---|
| Distributed artifact permission dump shows `INTERNET` only (+ the AndroidX self-permission) | Verified — release/debug APKs and the AAB manifest, 2026-11-17 18:27; the 0.2.3 pair matches |
| Merged release manifest has `allowBackup="false"` and data-extraction rules | Verified — merged release manifest and AAB manifest |
| Dependency list contains no ad/analytics/crash SDK | Verified — shipped stack in [MODEL_LICENSES.md](MODEL_LICENSES.md) §0.1 |
| Answers on this page match the app's behaviour, row for row | See §1–§6 |
| Privacy policy published and identical in substance | [PRIVACY_POLICY.md](PRIVACY_POLICY.md) is the same text as the published page |

---

## 9. Caveats

- **The store's form wording can change.** The question labels on Google Play's Data safety form are revised
  from time to time; if a question has no exact match here, answer by the definition in §0, not by the label.
  Whether an earlier form was saved, and whether the form has since gained new questions, is not tracked here —
  re-read the form before completing it.
- The permission dump and manifest checks were run on the **2026-11-17 18:27 build** and repeated on the 0.2.3
  pair: the release and debug APKs and the AAB manifest list only `INTERNET` + the AndroidX self-permission, and
  both manifests show `allowBackup="false"`. No runtime behaviour was tested — see
  [../qa/BUILD_REPORT.md](../qa/BUILD_REPORT.md) §4–§5.
