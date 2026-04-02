# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [Unreleased]

### Added
- Debug log screen: accessible via a small bug-icon button at the bottom-left of the navigation rail; logs all API requests and responses with timestamps; toggle to enable/disable (persisted across restarts, off by default); clear and save-to-Downloads buttons; saved files are timestamped (`ahubby_debug_YYYY-MM-DD_HH-mm-ss.txt`)
- Automatic token re-authentication: when a Bearer token expires, the app silently re-authenticates using stored credentials and retries the failed request; if re-auth itself fails, the session is cleared and the user is redirected to the login screen

### Changed
- Login credentials (email + password) are now persisted alongside the token to enable automatic re-authentication

## [Previously Unreleased]

### Added
- App scaffold: single-module Gradle project, CI/CD build pipeline (lint, build, sign, nightly pre-release)
- Authentication: login screen with email/password, Bearer token stored in SharedPreferences
- Session persistence: app redirects to login screen if no valid token is present; logout clears stored credentials
- Phase 2 UI: NavigationRailView with four sections — GPX Routes, My Locations, Live (placeholder), Profile
- GPX Routes: sortable list with country filter, "Show in DMD2" / "✓ In DMD2" toggle (optimistic update)
- My Locations: list with category-colored indicator dot, per-location DMD2 toggle
- Profile: read-only display of account fields from login response (name, email, device, license, points)
- User profile fields (displayName, email, deviceName, mapLicense, communityPoints) persisted at login
