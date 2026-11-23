## Summary

<!-- What does this change do, and why? One or two paragraphs. -->

## Related issue

<!-- Fixes #123, or "none". -->

## Type of change

- [ ] Bug fix
- [ ] New feature
- [ ] Refactor or cleanup
- [ ] Documentation
- [ ] Build, CI or tooling
- [ ] Other (describe below)

## What changed

<!-- Bullet points. Mention behaviour the user can see and any limit that remains. -->

## Checklist

- [ ] The change is focused and the approach is explained above.
- [ ] `./gradlew :app:assembleDebug` succeeds.
- [ ] `./gradlew :app:testDebugUnitTest` passes, and the change is covered by a test where practical.
- [ ] `./gradlew :app:lintDebug` shows no new error; new warnings are explained.
- [ ] Instrumented tests were run and pass, if storage, export, manifest or an instrumented path changed.
- [ ] Every added or changed user-visible string exists in both `values/` and `values-ar/` (LocalizationParityTest passes).
- [ ] RTL layout was checked for UI changes.
- [ ] No new permission, network call, telemetry/analytics/ad SDK, cloud dependency or signing secret.
- [ ] No build output, `keystore.properties`, `local.properties`, APK/AAB or R8 `mapping.txt` is committed.
- [ ] Documentation, `CHANGELOG.md` under the Unreleased section and, when build evidence changed, `docs/qa/BUILD_REPORT.md` are updated.
- [ ] No competitor names, no unverified claims and no Google Play availability claim.
- [ ] Screenshots (if any) contain no personal photos or private data.

## Evidence

<!-- Paste the commands you ran and a one-line result for each, e.g.
     ./gradlew :app:testDebugUnitTest -> BUILD SUCCESSFUL, 94 tests, 0 failures
     Emulator: API 36, video preview and export dialog checked
     Not run: instrumented tests (no emulator available) -->

## Notes for reviewers

<!-- Known risks, follow-up work, or anything you could not verify. -->

For a security fix, read [SECURITY.md](../SECURITY.md) first and coordinate privately with the maintainer
instead of describing the vulnerability in this public pull request.
