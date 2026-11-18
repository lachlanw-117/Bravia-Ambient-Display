> Historical record, archived 14 November 2026. Intermediate plans and pending
> checks are dated evidence, not current instructions or a new backlog. See the
> [current overview](../../README.md) and [design evolution](../design-evolution.md).

# Household alerts - display-only candidate

Status: **deployed; notice display visually verified by the user; broader trial checks pending; not merged**. Work is on
`feature/household-alerts` from Bravia `bddf067`. The user approved commit/push,
TV installation and Bravia host rebuild/restart on 14 November 2026, explicitly
excluding merge, and explicitly requested that ADB debugging remain enabled.
That instruction supersedes the normal debugging-disable closeout for this session.
The unrelated untracked `HOME_SCREEN_ADS.md` is preserved.

## Deployment trial - 14 November 2026

The initial failure below is retained as trial history. Read-only HA UI inspection
subsequently established state `ready` and schema **string `"1"`**, not number `1`,
with two TV-routed items. The adapter's strict numeric comparison caused rejection.
Host-only fix `37ce0adfad0d603d21d4da1f276e7b54ec9a2128` accepts exactly `1` and
`"1"`; unavailable states, other versions and coercible spellings remain rejected.
37 host tests and exact-head CI run 43573554088 passed before the approved
Bravia-only stop/rebuild/start. Installed notice module SHA-256 is
`2b74753f115a03d554e537269eed007988900c5cb169b2de5e41565dba678cb2`;
server/Dockerfile/configuration and Android remain unchanged from the hashes below.
The previous diagnostic module is preserved privately at
`/backup/bravia-notices-pre-schema-fix-0249312`. No HA configuration, source rule,
helper value, reload or Core restart was needed. The existing Template editor draft
was restored after the read-only inspection.

After the fix the host starts without a new notice-failure log, but ADB screenshots
return empty image data. On 14 November 2026 (Australia/Sydney), the user answered
“Yes, both notices appear correctly” to the Preview check covering the living-room
TV lights visibility alert, one grouped update notice below weather and absence
of “Notices unavailable”. This verifies that scoped display result through user
observation; it does not establish untested lifecycle, remote or outage behaviour.

[PR #8](https://github.com/lachlanw-117/Bravia-Ambient-Display/pull/8) is open;
the user explicitly instructed **do not merge**. Initial runtime `fc28daf1e14a492b49abef45c2e1b34ff04f0ca9`
passed CI run 43572583738 and was installed on host and TV. Host-only diagnostic
revision `024931293c39b27860d61b304a43486759ec1090` subsequently passed 36 local
host tests and CI run 43573091484, and was installed without changing Android.
It logs only fixed failure labels, excluding arbitrary exception/response content.

| Check | Observed result |
|---|---|
| Prior host drift | All three previous package hashes matched the accepted home-style deployment; notice module absent |
| Host recovery | Private app backup `da805c77` succeeded; archive existence checked. Previous source package preserved at `/backup/bravia-package-pre-household-alerts-fc28daf`; initial alert package additionally preserved at `/backup/bravia-package-initial-alerts-fc28daf` |
| Host install | Four package hashes verified before/after copying; Bravia-only stop/rebuild/start succeeded. No Core restart, source/routing/helper changes or credentials inspected/disclosed |
| Host reachability/authentication | LAN health 200; unauthenticated notices and weather both 401 |
| Previous TV APK | Pulled before installation; SHA-256 `4f78ed62fa83291e51c3976c93716d5694b5909148ae81b0403d06838ad53c92`, matching the recorded prior accepted APK |
| TV install | Android 12; `install -r` succeeded. Installed APK pulled back and matched candidate SHA-256 `db65da664afe6dafaa74988b6a991d5e879e9d6e36536c44536bb205c087cc75` |
| Saved settings | Host address, masked access-key field, 60-second scenes and portrait-pair setting visibly retained; no saved credential contents read |
| Preview rendering | Whole photo, date/time and fresh Now/Today/Tomorrow rendered; tightened spacing and divider visible |
| Initial notice failure (historical) | TV showed “Notices unavailable”; host reported “Notice feed not ready”. Read-only inspection traced the mismatch to textual schema version `"1"`; corrected at runtime revision `37ce0ad` |
| Notice display after correction | **User-confirmed pass:** both natural notices appear correctly below weather, without the unavailable message. This is user visual evidence, not an agent screenshot or a complete lifecycle/remote acceptance pass |
| HA inspection | Named live package hash matches the earlier installed HA trial. Browser access host unavailable and native Chrome window minimised; user asked to restore it for read-only sensor inspection. Existing remote-session API authentication returned 401; no credential contents were inspected or changed |
| ADB | `adb_enabled=1`, TCP port 5555 and authenticated device connection verified. Left enabled at the user's explicit request; no debugging-off closeout is claimed |

Runtime package source hashes at diagnostic revision: server
`d3b8ed9df44cbe47d1ca37fdb223f4682479840038c42b552849be113e293775`,
notices `8152faaba812c24957ead63b168f45dca75c4f18173cfab5c5963a1fc5b567d6`,
Dockerfile `882368d21f8c3822b87dde050efbde6458699a47b4a655b151b567556777fa7b`,
configuration `c2abaa1ff2329c5f72556386aebfd5822b6f56e767d28e903a45230f6ff122c3`.
Private local APKs/screenshots and source manifests remain under ignored
`build/notice-preview`; no household photos or saved options are committed.

Scoped visual notice acceptance is recorded. Natural lifecycle/routing checks,
overflow on the real TV, controlled failure/recovery, restart continuity and
physical Back/ordinary idle checks for this candidate remain unverified. Keep
the PR open and unmerged as instructed; no additional device actions were taken
to record this confirmation. ADB remains enabled under the explicit exception.

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
The [offline comparison](../../docs/household-alerts-comparison.html) contains accepted
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

Require HA state `ready`, `attributes.schema_version` equal to number `1` or exact
string `"1"`, and a valid bounded item
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
