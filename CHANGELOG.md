# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [Unreleased]

### Added
- App scaffold: single-module Gradle project, CI/CD build pipeline (lint, build, sign, nightly pre-release)
- Authentication: login screen with email/password, Bearer token stored in SharedPreferences
- Session persistence: app redirects to login screen if no valid token is present; logout clears stored credentials
