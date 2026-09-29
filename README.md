# Mazkiplay Agent

Full-stack AI Agent Android app powered by OpenRouter.

## Architecture
- Android Kotlin client
- Node.js/Express API
- OpenRouter as the model gateway/AI brain
- API key stays server-side in `OPENROUTER_API_KEY`
- SQLite-ready persistence layer
- GitHub Actions release build

## Environment
Create `server/.env`:

```env
PORT=8080
OPENROUTER_API_KEY=your_key
OPENROUTER_MODEL=openai/gpt-5.6
CORS_ORIGIN=*
```

Run server:

```bash
cd server
npm install
npm start
```

## Android
Set the production API URL in `android/app/build.gradle` under `buildConfigField` or use a CI secret/configuration.

## Release
Push to GitHub. The workflow in `.github/workflows/android-release.yml` builds a release APK. For a production Play Store build, add a signing key through GitHub Actions secrets.

Never commit API keys or signing keys.
