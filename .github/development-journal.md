# Development Journal

## Software Stack

| Component | Choice |
|---|---|
| Language | Java 17 |
| Platform | Android minSdk 34 (API 34), targetSdk 35 |
| Build | Gradle 8.7, AGP 8.5.2 |
| HTTP | `HttpURLConnection` (stdlib, `java.net`) |
| JSON | `org.json` (bundled with Android) |
| UI | Material3 (`Theme.Material3.DayNight.NoActionBar`) + AndroidX AppCompat |

## Key Decisions

### Java over Kotlin
Consistency with the companion project (androdash). No Kotlin compiler overhead in CI.

### minSdk 34 (Android 14)
DMD devices run recent Android. API 34 avoids legacy codepaths and compatibility shims.

### No external HTTP library
`HttpURLConnection` (`java.net`) is available across all Android versions and covers all needs.
`java.net.http.HttpClient` (Java 11 SE module) is NOT available on Android — ART does not
include Java SE modules. Keeps the dependency footprint minimal — no OkHttp, no Retrofit.

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

### Phase 2
- NavigationRailView: persistent landscape nav (GPX Routes, My Locations, Live placeholder, Profile)
- Fragment-per-section pattern: each Fragment owns its own executor and data load lifecycle
- Optimistic toggle UX: state flipped immediately in the model and adapter; reverted + Snackbar on API failure
- Country filter: `MaterialAlertDialogBuilder` single-choice list derived from loaded GPX data
- Category color system: dot-shaped `View` with runtime `setColorFilter()` on a `circle.xml` shape drawable
- Toggle label: "Show in DMD2" (inactive) / "✓ In DMD2" (active) — controls sync to DMD2 navigation device

### Phase 3 (planned)
- GPX upload via `hub.dmdnavigation.com` (hub session cookie required)
- Location delete
- Live location tab: display own position + group members' live positions

## Live Location API (researched via ADB + APK analysis)

DMD broadcasts location by polling `POST /api/location_update.php` at ~1 Hz. The endpoint
accepts `{"data":{"_id":"<user_id>","lat":...,"lon":...,"speed":...,"heading":...,"altitude":...,"accuracy":...}}`.
Rate limited at roughly 500 ms minimum interval.

Group members' live positions are read via `GET /api/group_proxy.php?action=members&group_id=<id>`,
which returns an array of `UserInfo` objects with `latitude`, `longitude`, `speed`, `heading`, etc.
The `active_group_id` comes from the login response (empty when not in a group).

Navigate-to: `GET /api/navigate-to.php` polls for navigation targets pushed by group members;
`POST` with `action=clear` dismisses them.

The `location` field in the login response is a custom-encoded string (format not yet decoded).
The plain `latitude`/`longitude` floats are only exposed in the group members endpoint.

DMD uses OkHttp3 WebSocket internally for real-time group updates (`groupRideSlowScheduler`),
but the WebSocket URL is constructed at runtime and is not stored as a literal in the APK.
The HTTP polling path (`location_update.php`) is sufficient for the aHubby Phase 3 use case.
