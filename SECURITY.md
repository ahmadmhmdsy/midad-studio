# Security Policy

Midad Studio is an offline-first Android creative studio. It has no backend, no user accounts, no cloud processing
and no advertising or analytics SDKs: media is processed on the device, and the only network use is a
user-initiated AI model download. Security and privacy reports are taken seriously.

The current release is **0.2.3** (versionCode 5), a **signed pre-release**. It is **not published on Google
Play**; the signed APK and AAB are distributed only as GitHub Release assets. That status does not make the app
less sensitive to report against - it means only the signed artifacts from the releases page are supported.

## Supported versions

| Version line | Supported |
|---|---|
| **0.2.x** (current: 0.2.3, versionCode 5) | Yes - fixes ship in a new signed 0.2.x release |
| 0.1.x and earlier | No - internal baselines, never released |

Only builds signed with the project's upload key and downloaded from the
[releases page](https://github.com/ahmadmhmdsy/midad-studio/releases) are supported. A locally built, repackaged
or debug-signed APK is not something we can diagnose; if you install one, verify the artifact yourself.
Older builds are not patched - upgrade to the current release before reporting.

## Reporting a vulnerability

**Do not open a public issue for a security or privacy problem.**

The only private reporting channel is **GitHub Security Advisories**:
[Report a vulnerability](https://github.com/ahmadmhmdsy/midad-studio/security/advisories/new)
(repository **Security** tab -> **Report a vulnerability**). It supports a private advisory thread and a private
attachment for proof-of-concept files.

This project has **no security email address** and **no bug bounty**. If you cannot use Security Advisories, open a
public issue that asks the maintainer to arrange a private channel - **without** any technical detail, proof of
concept or personal data - and wait for a reply before sending the report.

A useful report includes:

- the affected version and versionCode (the app's Settings screen shows the version) and how you installed it;
- the Android version and device or emulator used;
- the area involved: media in/out, export, drafts/projects, AI model download or storage, permissions, intent
  handling, or release/signing configuration;
- a description of the impact and of the attacker model (who would have to do what, and what is exposed);
- reproduction steps or a minimal proof of concept, and whether a crafted file or another app is required;
- any suggested fix or hardening you have in mind.

## What to expect

- This is a single-maintainer project. Security work is **best-effort**: there is no response SLA and no bug
  bounty or payment of any kind.
- We aim to acknowledge a report within 7 days and to agree a disclosure timeline with you.
- Please give us reasonable time to release a fix before publishing details. Coordinated disclosure through a
  GitHub Security Advisory is welcome once a fix is available.
- We will credit you in the advisory and release notes unless you ask to stay anonymous.
- Please test only on devices, emulators and data you own or have explicit permission to test, and do not attempt
  to access other people's media or devices.

## Scope

### In scope

- **Local data protection** - drafts and projects in app-private storage, backup and device-transfer exclusion,
  exported files, temporary files, and the non-exported `FileProvider` (could another app gain read access it
  should not have?).
- **Permissions and components** - an unexpected permission request, an exported component that should not be
  exported, an overly broad content-URI grant, or a manifest/merge change that weakens the current posture
  (`INTERNET` only, no backup, no media/storage permissions).
- **AI model download integrity** - bypassing or weakening the HTTPS source and SHA-256 verification, model
  substitution, writing outside the app's model directory, a partially downloaded file being treated as installed,
  or a model file obtained from an unintended source.
- **Privacy claims** - any telemetry, analytics, tracking or network request that contradicts the README, the
  privacy page or the model-license documentation, or any media leaving the device.
- **Untrusted input** - a crafted image, video, project index or model causing a crash that destroys data, escapes
  an app-private directory or crosses into another app; malicious intents aimed at the app's activities.
- **Release and signing configuration in this repository** - exposed secrets, a release configuration that
  produces a debuggable/backup-enabled build, or a path that lets an unsigned or debug-signed artifact be mistaken
  for a release.

### Out of scope

- Vulnerabilities in Android, the device vendor software, or in third-party libraries and models (Media3, LiteRT,
  AndroidX, Kotlin and the MediaPipe/MODNet model files). Report those upstream; tell us if the app's specific use
  makes a known issue exploitable.
- Issues that require physical access to an unlocked device, a rooted device, an unlocked bootloader or ADB access
  to the app's private data - that is outside the app's threat model.
- Model accuracy, bias, quality or licence questions - open a normal issue (or a licence question) instead.
- Crashes, performance problems and export failures without a security or privacy impact - report those as normal
  bugs.
- Reports produced only by an automated scanner without a demonstrated impact on this app.
- Social engineering, spam, physical theft, and anything targeting GitHub rather than the app.

## Model download integrity: expected behaviour

Because model downloads are the app's only network surface, this is how they are expected to behave:

- The catalogue is bundled with the app. A model is fetched only when the user asks for it; there is no automatic
  or background download and no silent update.
- Each entry is pinned to an HTTPS URL and an exact byte size and SHA-256 digest in the bundled manifest. The
  downloaded file is verified before it is installed, and a mismatch is rejected rather than loaded.
- Models are stored in app-private storage. The app does not load models from shared storage, from another app, or
  from a user-supplied file path.
- Two bundled models ship inside the APK/AAB (MediaPipe Selfie Segmenter square and landscape); four models are
  download-only (Selfie Multiclass, Hair Segmenter, DeepLab-V3 and MODNet).

A way to make the app load or install a model that does not match the pinned digest is a valid security report.

## Safe harbour

If you make a good-faith effort to follow this policy - test only on your own devices and data, avoid privacy
violations and service disruption, and give us a reasonable chance to fix the issue before disclosing - we will not
pursue legal action against you for the research itself, to the extent permitted by applicable law. This statement
does not override local law, and it does not authorise testing third-party services.
