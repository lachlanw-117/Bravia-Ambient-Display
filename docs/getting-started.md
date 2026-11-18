# Getting started

This source project builds a native TV screensaver and an HA OS local app.
There is no released installer or universal Sony compatibility claim. Local
checks do not connect to a TV or HA.

## Requirements

| Component | Requirement |
| --- | --- |
| Host checks | Node.js 22 or later; no npm dependencies to install |
| Android build | Windows, JDK 21, Android API 35 and build-tools 35.0.0 |
| App runtime | API 26 minimum; recorded device is a Sony X90K on Android 12 |
| Household host | HA OS; add-on supports aarch64 and amd64; recorded host is a Pi 5 |
| Album | JPEG, PNG or WebP; at most 250 files, 8 MiB each and 256 MiB total |
| Notices | Version-1 `sensor.household_attention_tv`; see the [contract](../HOUSEHOLD_ALERTS.md#read-only-contract-and-freshness) |

HA owns source rules and phone delivery in the companion project. Building this
repository does not install them or validate other firmware.

## Local checks and package

From the repository root:

```sh
npm test
npm run check:docs
node tools/package-addon.mjs
node --check build/bravia_ambient/server.mjs
node --check build/bravia_ambient/notices.mjs
```

The generated folder contains the Dockerfile, HA app definition and both server
modules. Generation does not install, start or reload anything.

## Android build

With existing tools, pass the JDK and SDK locations explicitly:

```powershell
./tools/build-android.ps1 -JavaHome $env:JAVA_HOME -SdkRoot $env:ANDROID_HOME -Unsigned
```

Output: `build/android/wright-ambient-unsigned.apk`. This aligned package validates
the build; installation requires signing. Unsigned mode exits before signing-key
lookup or creation and leaves an existing signed candidate alone.

If Windows blocks local scripts under its execution policy, invoke this script
with `powershell -NoProfile -ExecutionPolicy Bypass -File` followed by the same
path and arguments. This applies to that process only; do not change machine policy.

For a portable toolchain, the bootstrap verifies download checksums and prompts
for Android SDK licence acceptance, without changing global Java settings:

```powershell
./tools/bootstrap-windows.ps1
./tools/build-android.ps1 -Unsigned
```

The default location is ignored `.tools/android`. `-Tools PATH` still selects a
portable installation elsewhere; `-JavaHome` and `-SdkRoot` override its respective
locations. Missing prerequisites fail with a named path, without version fallback.

An approved local signed build uses the same command without `-Unsigned`:

```powershell
./tools/build-android.ps1
```

It retains the local debug-signing workflow and outputs
`build/android/wright-ambient-debug.apk`. Preserve the private signing identity
when updating an installation; changing it can require uninstalling and losing
settings/cache. Never commit or share the signing material.

## Prepare an installation

Read [maintenance](../MAINTENANCE.md). Device actions need named approval; they
are not part of the check/build scripts.

1. Manually export an album. Convert HEIC/RAW stills and exclude video, then copy
   supported files into `/share/bravia-ambient`. Publish complete files, not partial
   JPEGs. An empty source preserves the previous working cache.
2. Generate the package. Preserve the known-good package/APK and a private app
   backup, then review exact source and installed drift.
3. With approval, copy the package to HA OS's local-app folder (`/local_apps/` in
   the recorded HA OS 18.4 setup), rebuild/start Bravia and install the signed TV
   APK while retaining app data.
4. Set `access_key` (random, at least 24 characters), `weather_entity` and `time_zone`
   through HA app options. Select a daily-forecast-capable entity; the household
   uses BOM and `Australia/Sydney`. Keep values private.
5. In TV Setup, enter `http://PI_LAN_ADDRESS:8099` or a suitable existing HTTPS
   address and the host key, never an HA token. Save and open Preview photos.
6. Select Wright family photos using the system screensaver picker if exposed.
   A hidden picker requires firmware-specific inspection, not blanket package
   removal or guessed privileged commands.
7. Verify the intended display and first-input exit, then record actual results,
   rollback identities and session settings. See [validation](../VALIDATION.md).

Keep the host on a trusted LAN. HTTP is unencrypted; a key does not encrypt the
transport. `/health` proves reachability, not HA weather/notice success. The host
uses HA-provided runtime authentication internally without sending that token to TV.

## Normal operation and recovery

Android owns idle timing, Home and power. Scene duration (10-300 seconds) does not
control idle entry. One remote input exits; no in-display hide/acknowledge controls
are implemented.

Caches retain last-known content during failure. Notice freshness ages the last
successful HA retrieval; timed messages still expire offline. See [architecture](architecture.md).

Approved rollback restores the previous package and signed APK while preserving
unrelated edits, app data and photos. Clearing data/uninstalling is a separate
action. ADB is unnecessary for normal playback; the owner explicitly requested
it remain enabled on 14 November. This guide does not change that setting.
