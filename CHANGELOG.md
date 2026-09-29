# Changelog

## 0.3.2 - 2026-09-30
- Fixed CI release signing by generating a temporary keystore inside GitHub Actions.
- Release build now passes explicit signing properties to Gradle.
- Release artifact is versioned as `Mazkiplay-Agent-v0.3.2.apk`.


## 0.3.1 - 2026-09-30
- Fixed CI release signing so the workflow produces an installable `app-release.apk`.
- Added APK existence verification before publishing the GitHub Release.
- Added release artifact retention for 14 days.
- Documented the Android client + Node/Express backend full-stack layout.
- Marked the CI APK as internal/testing signed; a private production keystore is required for Google Play distribution.

## 0.3.0 - 2026-09-30
- Direct OpenRouter integration from the Android app.
- API key is entered by the user and stored locally on the device.
- Configurable OpenRouter model, max output tokens, and system prompt.
- Persistent local conversation history.
- Quick actions for planning, CV/job applications, product ideas, and translation.
- Token usage status after successful responses.
- Android chat no longer depends on the sample hosted backend.

## 0.2.0
- Initial Android agent shell and optional gateway backend.
