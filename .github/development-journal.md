# Development Journal

## Software Stack

| Component | Choice |
|---|---|
| Language | Java 17 |
| Platform | Android minSdk 34 (API 34), targetSdk 35 |
| Build | Gradle 8.7, AGP 8.5.2 |
| HTTP | `java.net.http.HttpClient` (stdlib) |
| JSON | `org.json` (bundled with Android) |
| UI | Material3 (`Theme.Material3.DayNight.NoActionBar`) + AndroidX AppCompat |

## Key Decisions

### Java over Kotlin
Consistency with the companion project (androdash). No Kotlin compiler overhead in CI.

### minSdk 34 (Android 14)
DMD devices run recent Android. API 34 gives access to `java.net.http.HttpClient` without
compatibility shims and avoids legacy codepaths.

### No external HTTP library
`java.net.http.HttpClient` (available from API 26+) covers all needs. Keeps the dependency
footprint minimal — no OkHttp, no Retrofit.

### No external JSON library
`org.json` is bundled with the Android runtime. No additional dependency needed for parsing
API responses.

### Dual auth backends
- `app.advhub.net`: programmatic login via Bearer token — used for all data API calls
- `hub.dmdnavigation.com`: session cookie only (Cloudflare Turnstile blocks programmatic login)

Phase 1 implements only the `app.advhub.net` path. The hub session (`dmdub_session`) will be
required in Phase 2 for GPX upload.

### Token expiry handling
The `user_token` expires; a 401 response signals expiry. The app clears the stored token and
redirects to the login screen. Passwords are not persisted.

## Core Features

### Phase 1
- Login / session persistence via `AuthStore` (SharedPreferences)
- Bearer token authentication against `app.advhub.net`

### Phase 2 (planned)
- GPX Manager: list, toggle "Loaded to DMD Map", upload
- Locations: list, delete
