# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

**aHubby** — Android companion app for [DMD Navigation Hub](https://hub.dmdnavigation.com).

### Roadmap

**Phase 1 (current):**
- App scaffold and build pipeline (adapted from `/home/sdk/androdash/.github/workflows/build.yaml`)
- Authentication with the DMD Hub (`app.advhub.net` Bearer token)
- Feature discovery / API state

**Phase 2:**
- UI concept discussion
- GPX Manager and Locations UI

**Phase 3:** TBD

## Implementation

Android app. Build pipeline CI/CD via GitHub Actions.

**Do not attempt to build Android projects locally.** All builds are handled by CI/CD. AGP cannot be accessed due to firewall restrictions — do not try to work around this.

## Commands

```sh
./gradlew lint            # static analysis
./gradlew assembleRelease # build release APK (CI only)
./gradlew test            # run unit tests
./gradlew test --tests "com.example.FooTest#methodName"  # run single test
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

## Code Style

- KISS. Write testable code with unit tests covering assumptions and edge cases.
- No database/schema migration code during development (version < 1.0.0).
- Always use the latest available library versions.
- Before implementing a feature from scratch, check whether libraries and frameworks already in use provide built-in support — possibly in a different form. Explain what's available and let the user decide.

## Auth

Two backends, two auth paths:

- **`app.advhub.net`** (Bearer token): `POST /api/dmd_connector.php` with email/password → `user_token`. Use for GPX list/toggle, locations list/create/update/delete.
- **`hub.dmdnavigation.com`** (session cookie): No programmatic login — Cloudflare Turnstile CAPTCHA blocks it. User must supply `dmdub_session` cookie. Use for GPX upload.

## API Reference

See `api_specification.md` for the full API spec covering:
- GPX Manager (`/api/gpx-manager.php`) — list, toggle, create folder, move, delete
- GPX upload (`/account/profile/gpx/`) — multipart form, hub session only
- Community collection (`/api/gpx-collection/`)
- Download/export (`/api/gpx-download/`, `/api/gpx-export.php`)
- Locations (`/account/profile/locations/`) — form-based, requires CSRF token
- Android app API proxy (`app.advhub.net`) — preferred programmatic path, JSON, Bearer token
