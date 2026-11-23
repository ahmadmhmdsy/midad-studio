<div align="center">

# Midad Studio documentation

Public documentation for **Midad Studio / استوديو مِداد** — the offline-first Android creative studio.

[Repository README](../README.md) · [README بالعربية](../README.ar.md) · [Licence](../LICENSE)

</div>

This folder holds the documentation that ships with the repository: product notes, build evidence, the privacy
policy, model licensing and the store-asset guide. Everything here describes the build published as
**v0.2.3** unless a page says otherwise.

## Product documentation

- [Preview and results](product/PREVIEW_AND_RESULTS.md) — what the result screens show, what each export
  produces, and the difference between a preview and a finished file.
- [Projects](product/PROJECTS.md) — how editable, app-private projects work and the limits video projects keep
  (3 minutes, 5 clips).
- [Data safety](product/DATA_SAFETY.md) — the permission inventory, the single network path, and the answers
  behind the store's Data safety section.
- [Privacy policy](product/PRIVACY_POLICY.md) — the full policy in English and Arabic.
- [Model licences](product/MODEL_LICENSES.md) — licence matrix for the six on-device models, with sources.
- [Licence texts](product/licenses/) — verbatim upstream licence copies shipped with the repository.
- [Model manifest](product/model_manifest.json) — the machine-readable model metadata the app checks downloads
  against.

## Build and quality

- [Build report](qa/BUILD_REPORT.md) — verification evidence for the v0.2.3 build: unit and instrumented test
  results, lint triage, release-gate output and archive checks.

## Research

- [Model catalogue](research/MODEL_CATALOGUE.md) — the segmentation models that were evaluated, with their sizes,
  licences and the reason each one is or is not bundled.

## Store assets

- [Store assets guide](store/README.md) — app icon, feature graphic, screenshot inventory, alt text and copy
  notes.
- [Screenshots from the v0.2.3 build](store/screenshots-v023/) — seven 1080×2400 captures from the signed
  release build, used in the English README; provenance and hashes in that folder's README.
- [Screenshots from the 0.2.0 pre-release](store/screenshots/) — the Arabic-UI 1080×1920 set used in
  README.ar.md and the store guide; some labels and numerals differ in v0.2.3.

## Repository documents

- [README (English)](../README.md) · [README (العربية)](../README.ar.md)
- [CHANGELOG](../CHANGELOG.md) — release history.
- [CONTRIBUTING](../CONTRIBUTING.md) — how to report a bug, propose a change or open a pull request.
- [CODE_OF_CONDUCT](../CODE_OF_CONDUCT.md) — the behaviour expected in this project's spaces.
- [SECURITY](../SECURITY.md) — how to report a vulnerability privately.
- [THIRD_PARTY_NOTICES](../THIRD_PARTY_NOTICES.md) — third-party components and their licences.
- [LICENSE](../LICENSE) — source-available notice and terms; this is not an open-source licence.

## Website

The GitHub Pages site is built from this folder and published at
[ahmadmhmdsy.github.io/midad-studio](https://ahmadmhmdsy.github.io/midad-studio/), with an Arabic edition at
[.../ar/](https://ahmadmhmdsy.github.io/midad-studio/ar/) and the privacy policy at
[.../privacy/](https://ahmadmhmdsy.github.io/midad-studio/privacy/). The pages themselves live in
[index.html](index.html), [ar/index.html](ar/index.html) and [privacy/index.html](privacy/index.html).

## Licence of this documentation

The prose in this folder is © 2026 Ahmad Mahmoud, all rights reserved, and follows the repository's
[source-available terms](../LICENSE). Third-party licence texts in [product/licenses/](product/licenses/) remain
under their own licences.
