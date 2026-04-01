# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

**aHubby** — Android companion app for [DMD Navigation Hub](https://hub.dmdnavigation.com).

### Roadmap

**Phase 1 & 2 (complete):**
- App scaffold and build pipeline
- Authentication with `app.advhub.net` Bearer token
- NavigationRail UI: GPX Routes, My Locations, Live (placeholder), Profile

**Phase 3 (planned):**
- GPX upload via hub session cookie
- Location delete
- Live location tracking

## Implementation

Android app. Build pipeline CI/CD via GitHub Actions.

**Do not attempt to build Android projects locally.** All builds are handled by CI/CD. AGP cannot be accessed due to firewall restrictions — do not try to work around this.

## Commands

```sh
./gradlew lint            # static analysis
./gradlew assembleRelease # build release APK (CI only)
./gradlew test            # run unit tests
./gradlew test --tests "de.codevoid.ahubby.FooTest#methodName"  # run single test
```

## Git

- user.name = c0dev0id
- user.email = sh+git@codevoid.de
- Always rebase the working branch onto the latest `main` at the end of a task. Resolve conflicts during the rebase.
- Remove all lines containing the word "claude" from commit and pull request messages.
- If a `.gh_token` file is present, use it to access GitHub and read CI/CD workflow results.

## Changelog

Maintain `CHANGELOG.md` following [Keep a Changelog](https://keepachangelog.com). Update after each development task with a human-readable summary of what changed. Do not list individual commits. Skip trivial or non-user-facing changes.

## Development Journal

Maintain `.github/development-journal.md` containing:
- Software stack information
- Key decisions (context and rationale for future work)
- Core features

## Architecture

**Stack:** Java 17, minSdk 34, Material3 + AndroidX AppCompat, no external HTTP or JSON libraries.

**Deliberately excluded:** Retrofit, OkHttp, Room, LiveData, ViewModel, Coroutines, Hilt. HTTP is done with `java.net.HttpURLConnection`; JSON with `org.json` (bundled with Android). Data is not persisted between sessions — all list data lives in memory.

**Package layout (`de.codevoid.ahubby`):**
- `api/ApiClient.java` — all HTTP calls, both auth and data; returns parsed results or throws
- `auth/AuthStore.java` — SharedPreferences wrapper; stores Bearer token and cached profile fields
- `model/` — `GpxFile`, `HubLocation`; each has a static `parseList(String json)` factory
- `adapter/` — `GpxAdapter`, `LocationsAdapter`; each exposes a `ToggleListener` interface
- `fragment/` — `GpxFragment`, `LocationsFragment`, `LiveFragment`, `ProfileFragment`
- `LoginActivity.java`, `MainActivity.java` — two activities total

**Navigation:** `MainActivity` hosts a `NavigationRailView` and swaps fragments into a `FragmentContainerView`. On launch it checks `AuthStore.isLoggedIn()`; if false it starts `LoginActivity` and finishes itself.

**Async pattern used in every data-loading fragment:**
1. Show loading indicator
2. Submit task to `ExecutorService` (single-thread)
3. Call `ApiClient` method (blocking)
4. `runOnUiThread` to update adapter or show error
5. Toggle actions use optimistic UI: update state immediately, revert + show `Snackbar` on failure

## Code Style

- KISS. Write testable code with unit tests covering assumptions and edge cases.
- No database/schema migration code during development (version < 1.0.0).
- Always use the latest available library versions.
- Before implementing a feature from scratch, check whether libraries and frameworks already in use provide built-in support — possibly in a different form. Explain what's available and let the user decide.

## Auth

Two backends, two auth paths:

- **`app.advhub.net`** (Bearer token): `POST /api/dmd_connector.php` with email/password → `user_token`. Use for GPX list/toggle, locations list/create/update/delete. Each login invalidates the previous token.
- **`hub.dmdnavigation.com`** (session cookie): No programmatic login — Cloudflare Turnstile CAPTCHA blocks it. User must supply `dmdub_session` cookie. Use for GPX upload.

## API Reference

See `api_specification.md` for the full API spec covering:
- GPX Manager (`/api/gpx-manager.php`) — list, toggle, create folder, move, delete
- GPX upload (`/account/profile/gpx/`) — multipart form, hub session only
- Community collection (`/api/gpx-collection/`)
- Download/export (`/api/gpx-download/`, `/api/gpx-export.php`)
- Locations (`/account/profile/locations/`) — form-based, requires CSRF token
- Android app API proxy (`app.advhub.net`) — preferred programmatic path, JSON, Bearer token
