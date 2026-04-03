# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [Unreleased]

### Added
- GPX download: each route item now has a download button that saves the `.gpx` file directly to the Downloads folder via MediaStore
- GPX upload button in the routes header (shows "not yet available" — endpoint pending discovery)

### Changed
- All data API calls migrated to the `/api/ios/` namespace on `app.advhub.net` — cleaner, more complete, JSON-only, no CSRF required
- GPX list now traverses the server's folder hierarchy to collect all files regardless of folder nesting
- Location coordinates changed from a combined `"lat, lon"` string to separate `latitude`/`longitude` fields throughout the model and create/edit form
- Location coordinate field in the add/edit form is now display-only (populated from Nominatim search); no longer manually editable

## [Previously — Phase 2]

### Added
- Edit Location: tapping a location in the list opens the creation form prefilled with its data; save calls the update API; a Delete button with confirmation dialog removes the location
- Create Location: "Add" button in the Locations header opens a form to create a new location; search field calls Nominatim to find addresses and POIs and auto-populates coordinates, country, and continent; category dropdown with common types; saves via the Bearer token API and refreshes the list on success
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
- Live tab: GPS position display, active group ID from login response, toggle to start/stop sending location to the hub every second
