# Mazkiplay Agent

Mazkiplay Agent is an Android AI-agent client with an OpenRouter-backed server gateway.

## Current release: 0.2.0

### Included
- Android Kotlin client with dark responsive chat UI
- Configurable API endpoint from in-app settings
- Active-session conversation history
- Clear conversation control
- OpenRouter model gateway
- Server-side API-key handling
- Input validation and message/output limits
- Health endpoint
- GitHub Actions release build

### Architecture

Android App -> Mazkiplay API Gateway -> OpenRouter -> selected AI model

The OpenRouter secret is not embedded in the APK or committed to GitHub.

## Server configuration

Create server/.env:

PORT=8080
OPENROUTER_API_KEY=your_key
OPENROUTER_MODEL=openai/gpt-5.6
CORS_ORIGIN=*
MAX_MESSAGES=40
MAX_TOKENS=2048
APP_URL=https://your-production-domain.example

Run:

cd server
npm install
npm start

## Android configuration

The development default endpoint is http://10.0.2.2:8080 for the Android emulator.
For a real device or production deployment, open Settings inside the app and set the deployed API URL. Use HTTPS in production.

## Release security

Do not commit OpenRouter API keys, database credentials, JWT secrets, signing keystores, or private keys.
The current GitHub Actions build produces an unsigned release APK. A production or Play Store release should use a permanent signing key stored in GitHub Actions secrets.

## Roadmap

1. Authenticated user accounts
2. Encrypted per-user memory and database persistence
3. Tool registry with explicit permissions
4. Web/search and URL tools
5. File/PDF analysis
6. Scheduled tasks and notifications
7. Streaming responses
8. Usage metering, credits and subscriptions
9. Multi-model routing through OpenRouter
10. Signed APK/AAB release pipeline

## License

MIT