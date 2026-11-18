# Household notices

The installed display-only feature shows HA-approved notices below weather.
The user confirmed both natural notice rows on 14 November 2026. PR #8 remains
open and unmerged by request. Exact component revisions and scoped evidence are
in the [dated trial](docs/history/household-alerts-2026-11-14.md); current evidence
limits are in [validation](VALIDATION.md).

## Ownership and display

HA owns the inbox, source meanings, occurrence lifecycle, routing switches and
pauses in the [companion project](https://github.com/lachlanw-117/Wright-Home-Assistant).
The Bravia consumer supports the destination schema rather than a hard-coded
source inventory. No private inbox, HA evidence or phone routing records reach TV.
The [trial record](docs/history/household-alerts-2026-11-14.md) distinguishes
companion branch work from the installed HA revision and its acceptance limits.

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
system-owned. Navigation, hiding, acknowledgement and automatic paging are outside the current scope.

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


<a id="local-validation-and-remaining-acceptance"></a>

## Validation and evidence limits

The feature has 37 synthetic host tests and 278 JVM assertions. The
[validation guide](VALIDATION.md) distinguishes automated coverage from recorded
device observations. Natural lifecycle/routing changes, real-TV overflow,
controlled outages and restart continuity remain evidence limits, not a new
phase plan. Further runtime work and merge need their own review and approval.

## Deployment trial - 14 November 2026

The [archived trial](docs/history/household-alerts-2026-11-14.md#deployment-trial---14-november-2026)
preserves deployment/rollback identities, initial failure, schema correction and
user confirmation. Its intermediate instructions and pending checklist are
historical; the owner subsequently requested ADB remain enabled and no merge.

<a id="household-alerts---display-only-candidate"></a>
