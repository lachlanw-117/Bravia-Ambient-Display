# Household alerts - display-only candidate

Status: **authorised local implementation; implemented locally; not deployed or
device-verified**. Work is on `feature/household-alerts` from Bravia `bddf067`.
The user subsequently approved commit/push and TV deployment on 14 November 2026,
explicitly excluding merge. Host rebuild/restart and temporary debugging scope
are being clarified separately. Deployment has not yet occurred. The unrelated
untracked `HOME_SCREEN_ADS.md` is preserved.

## Ownership and display

[HA PR #11](https://github.com/lachlanw-117/Wright-Home-Assistant/pull/11) owns
the inbox, source meanings, occurrence lifecycle, routing switches and pauses.
Its trial record identifies installed runtime `6b36c4a`; head `3cc4573` contains
later work, including sources not yet installed. HA acceptance remains incomplete.
The Bravia consumer supports the schema rather than hard-coding that nine-row
source inventory. No private inbox, HA evidence or phone routing records reach TV.

The bottom-right sidebar shows up to three two-line titles, ordered by important,
warning, info; ties use oldest first detection, then occurrence identity. Titles
wrap/ellipsise at existing readable type size. Additional items show
`+N more · View in HA`, without paging. Grouped upgrades use one distinct-component
count, preserving HA's last-confirmed details, with uncertainty wording as needed.
An uncertain upgrade record with no retained components says “Update status uncertain”.
Marker colours use the established warm/green palette; severity titles retain
the actual condition text. Accessibility includes the full received notice titles.

The clock-to-Now gap is reduced by 1.6% of screen height. The weather divider is
2.2% of screen height below the last rendered check-text baseline, moving with
wrapped forecasts/check text. Notice capacity is calculated from the remaining
space, reserving room for overflow and stale status rather than shrinking text.
The [offline comparison](docs/household-alerts-comparison.html) contains accepted
baseline, empty, single, mixed, long-title and overflow cases. Optional controls
show long weather, narrow width and stale state. It uses synthetic data and
browser font metrics; it is not an Android render or live screenshot.

Every remote input still dismisses Preview/screensaver. Home/power remain
system-owned. Navigation, hiding, acknowledgement and automatic paging are deferred.

## Read-only contract and freshness

`GET /notices` uses the existing host bearer key and no-store response headers.
Other HTTP methods are rejected. The host requests only
`GET /api/states/sensor.household_attention_tv`, with its existing HA credential;
the TV continues to store only the host key. No new add-on options are required.

The response is `{version: 1, fetchedAt, stale, items}`. `fetchedAt` is the ISO UTC
time of the last successful HA retrieval, or null before any successful snapshot.
Items expose only `occurrenceId`, `category`, `kind`, `severity`, `title`, `active`,
`firstDetected`, `lastValidObservation`, `resolvedAt`, `expiresAt`, `quality`, and
optional `updateCount`. Item times are epoch seconds; nullable observation/recovery
times remain null. Only events have an expiry; an inactive recent event can still
display until that deadline. Component IDs/versions and raw source evidence are
used only inside the host to build the grouped count and are not returned.

Require HA state `ready`, `attributes.schema_version: 1` and a valid bounded item
array. At most 64 records, 300-character titles, 256-character occurrence IDs,
256 upgrade details, 256 KiB upstream body and 60,000-byte sanitised snapshot are
accepted. The whole snapshot fails on invalid records or duplicate identities;
failure never becomes a successful empty feed. HTTP redirects are rejected and
upstream requests time out after ten seconds. Android bounds its response/cache
to 64 KiB and independently validates the public schema.

Host and TV poll independently every 15 seconds, immediately on startup/entry.
Photo/weather IO cannot block notice polling, and notice updates do not depend
on the weather minute-change optimisation. Serving starts without waiting for
album synchronisation/weather retrieval. TV requests and schedulers stop on exit.
HA's own periodic source evaluation can add latency beyond these polling intervals.

Both sides atomically persist the last valid snapshot in private runtime storage.
Host storage is `/data/cache/notices.json` in the installed add-on; TV storage is
app-private `ambient/notices.json`, scoped to the configured host address. A fresh
empty snapshot removes previous items. An unavailable newly restarted host cannot
erase the TV's retained last-known snapshot. Faults are not resolved locally.
Events expire at HA's deadline even offline, independently of their active flag.

At two minutes without successful HA retrieval, show “Notices may be out of date”
and retain last-known unresolved notices. The TV also ages this timestamp when
its own connection fails. An explicit stale flag or future retrieval time is
uncertain immediately. Without any valid snapshot, show “Notices unavailable”.
A fresh empty snapshot leaves the area blank, without an all-clear claim.

## Local validation and remaining acceptance

Host tests exercise production sanitisation/fetch/cache logic, authentication,
schema/body limits, grouped updates, duplicate records, routed snapshot removal,
event deadlines, stale boundaries, persistence failure, restart restoration,
overlapping polls and successful empty recovery. JVM tests exercise the production
normalised-map parser, ordering, capacity/overflow, expiry and restored snapshots.
The Android JSON bridge, AtomicFile persistence and lifecycle need actual Android
acceptance; the JVM tests do not pretend to run Android storage or graphics.

Local checks: `npm.cmd test`, `tools/build-android.ps1 -Tools EXISTING_TOOLCHAIN`,
`node tools/package-addon.mjs`, packaged-module syntax/import checks and
`git diff --check`. Existing synthetic photo/weather tests remain part of the run.
The existing private toolchain is reused without inspecting signing material.
Candidate APK: `build/android/wright-ambient-debug.apk`; generated host package:
`build/bravia_ambient`. Build outputs, screenshots and browser profile are ignored.

Results on 14 November 2026: **35 host tests and 278 JVM assertions passed**
(15 cache policy, 208 photo presentation, 25 weather and 30 notice assertions).
Android resources/Java/DEX, APK signing/signature verification, package generation
and packaged-module syntax/import checks passed. Only the existing Java 8 and
deprecated fullscreen API warnings remain. Standard and combined stale/long-weather/
narrow-width synthetic comparisons were rendered locally and visually inspected:
titles wrap/ellipsise, overflow remains visible and weather does not overlap notices.
These are offline design checks, not device acceptance. Local diff whitespace and
new-document link checks passed; CI has not run for this unpublished working tree.

Before deployment, obtain separate Git publication approval, review the exact
feature-branch commit and keep its PR open through the trial. Obtain named host
rebuild/restart and TV install/debugging approval; no Core restart is required by
this source change. Verify existing runtime drift and preserve known-good private
package/APK and runtime snapshots before replacement. The host trial will also
include main's merged but previously undeployed forecast-unit cache fix.

Pending device acceptance:

- Actual authenticated HA-TV-host path; natural notice changes, grouping, expiry,
  routing suppression/recovery and successful empty feed. Do not manufacture HA
  entities or disconnect household devices to obtain examples.
- Couch-distance title wrapping, long weather/check text, notice capacity and
  overflow. Offline comparisons cannot establish Sony font metrics.
- Photo fitting/pairing, transitions, continuation, weather freshness, idle entry,
  first-input/Back dismissal and system-owned Home/power; relevant underlying
  playback/HDMI checks remain explicit rather than assumed.
- Host/TV restart cache continuity and failed-connection recovery only within a
  separately approved trial scope; local synthetic evidence is not a device pass.
- Debugging closeout, refused debugging port and ordinary idle/exit with debugging
  off. Record exact source revisions/hashes and user observations before merge approval.

Rollback with approval: restore the previous host package and matching known-good
APK while preserving app data, signing identity, photos and private cache backups.
The added notice cache is independent of the older app and can remain unused;
do not clear app data. Keep both runtime PRs' acceptance records distinct.
