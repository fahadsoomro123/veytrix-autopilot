# VEYTRIX Phase 5 — Native OTA

## Scope

Add native, user-approved OTA discovery and installation to Android without a WebView.

## Release source

The app reads the latest published GitHub release from the public VEYTRIX repository and looks for the `veytrix-autopilot-release.apk` asset (with `autopilot-release.apk` accepted for compatibility). GitHub's release API exposes the latest release and release asset download metadata; the release asset API supports retrieving binary content and current release asset records expose a `digest` field. GitHub's release documentation describes `browser_download_url` for assets. See the official GitHub REST release documentation.

## Integrity gates

- HTTPS only.
- GitHub API host is exact `api.github.com`.
- Initial APK URL must be under the VEYTRIX GitHub release download prefix.
- Redirects are disabled and only expected GitHub release asset hosts are allowed.
- APK SHA-256 must match the release asset digest before staging.
- Package name must match `com.veytrix.autopilot`.
- Version code must be greater than the installed package.
- Installed signing certificate must match the update package signer or its signing certificate history.

## Android install flow

Android `PackageInstaller` is used instead of the deprecated `ACTION_INSTALL_PACKAGE` intent. The user is shown the platform's installation approval when required. Android may require the user to allow this source to install packages; the update surface links directly to the source-specific settings screen.

## Safety boundary

The update surface never auto-installs, never downgrades the app, and never bypasses Android's package-install security. Debug builds intentionally reject a production-signed release when their signing certificate differs.

## Rollback

Normal app update installs are monotonic by version code. A lower-version release is rejected. An effective rollback therefore requires a separately signed recovery release built from an older source revision with a newer version code; this is a future release-publishing capability, not a client-side downgrade bypass.

## Release publication integrity

The release publisher binds the published GitHub tag to the exact commit that produced the signed APK. Release tags are semantic-version formatted, existing tags are never overwritten, and OTA metadata is generated with structured JSON encoding rather than raw string interpolation. This prevents a published tag or its assets from being silently replaced by a build from a different source revision.

## Lock gate

Phase 5 is only lockable after the OTA contract, Android build, APK identity verification, signed release workflow validation, and on-device install/update test all pass.