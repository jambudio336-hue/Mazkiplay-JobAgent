# Mazkiplay Agent

Premium Android AI job-hunting workspace powered by OpenRouter.

## Release 0.4.0

### Dashboard
- Premium dark command-center UI
- Live date/time/year and online status
- Dashboard metrics and quick career actions
- Dedicated Job Hunting, Application Center and AI Agent pages

### Job Hunting
- Live public job-feed search
- Keyword and location filters
- Remote/on-site listing metadata where supplied by the source
- Application URL shown for each listing
- AI-assisted CV, ATS optimization, cover letters and application strategy

### Application Center
- Generate an application pack with AI
- Review before sending
- Opens the device email client for final user-controlled sending

The app deliberately does not silently submit applications or impersonate the user. Automated submission to a specific job platform requires that platform's supported API/authentication and explicit user authorization.

### AI
- Direct OpenRouter Chat Completions
- User-provided API key stored locally
- Configurable model, token limit and system prompt
- Persistent local conversation history
- Plan, Job CV, Ideas and Translate shortcuts

## Live / real-time behavior

The clock updates every second from the device clock. Job listings are fetched over HTTPS when the user searches. Real-time data depends on the upstream public feed and network connection; no app can guarantee connectivity when the device is offline.

## Security

No OpenRouter secret is hard-coded in the repository. The current local key storage should be upgraded to Android Keystore-backed encrypted storage before production distribution. The CI release key is temporary testing signing; use a permanent private signing key for Google Play.

## Build

Requirements: JDK 17, Android SDK compileSdk 36, Gradle 8.13.

## License

MIT
