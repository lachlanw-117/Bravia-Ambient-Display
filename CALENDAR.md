# Calendar sidebar candidate

Status: implemented locally, **not deployed; no device acceptance**. Work starts
from PR #8's head `0c7ce4518c0857fa9d4f1d24ad61925bb5f89d55` on
`feature/calendar-sidebar`. The draft PR targets `feature/household-alerts`, so its
diff contains the calendar work only. Neither that branch nor PR #8 is modified
or merged. If the notice candidate changes, review compatibility before rebasing
or retargeting. The unrelated untracked `HOME_SCREEN_ADS.md` is preserved.

## Behaviour and data contract

The existing authenticated, read-only host adds `GET /calendar`, using the same
bearer key and `Cache-Control: no-store`. Calendar IO has its own startup refresh
and 30-minute timer. It cannot block photo, weather or notice refreshes. Android
uses its own worker on entry and every five minutes, cancelling requests on exit.
No input handling, photo layout, photo sequencing or remote gestures change.

The public snapshot is `{schemaVersion: 1, fetchedAt, timezone:
"Australia/Sydney", stale, occurrences}`. Each occurrence has a SHA-256 opaque
`occurrenceId`, short `title`, `kind` (`birthday` or `occasion`), ISO civil
`localDate`, recomputed `daysUntil`, and optionally `displayAge`. English titles
containing “birthday” select that kind. Only all-day, single-date events are
supported. Titles containing control characters, email addresses, four-digit
years or numeric date patterns fail validation. Raw IDs, birth years, notes,
owners, attendee details and locations never enter the public snapshot.

The Graph adapter asks for a padded 400-day window so persisted data crosses a
year boundary. The public view and TV select today through the same date next
year (clamped to 28 February for a leap-day boundary), sorted by local date then
opaque ID. Up to three rows display. The TV recalculates at Sydney midnight and
on resume from its own clock; it never trusts a saved countdown. Recurrence,
including exceptions, moves and leap-day policy, belongs to Outlook. The adapter
does not invent annual dates from past events. Distinct same-day occasions remain
distinct; repeated IDs/date from page overlap deduplicate, conflicting versions
fail the whole refresh. A successful full snapshot replaces edited/deleted rows.

Existing notice ordering, measured capacity, overflow and status space are
allocated first. Coming up uses only the remainder, with no smaller text or
automatic paging. Calendar rows collapse before notices lose space. Today and
Tomorrow use the existing warm accent. Long calendar titles/details ellipsise;
accessibility includes their complete text. No cache says “Calendar unavailable”;
a successful empty snapshot says “No upcoming occasions”. At 24 hours without
success, show “Calendar may be out of date”. A future fetch time is uncertain
immediately. Stale rows remain available but past local days expire offline.
If no safe row/status fits, the calendar block is temporarily hidden.

Host persistence is `DATA_DIR/calendar.json` (installed default
`/data/cache/calendar.json`), independent of other caches. A source fingerprint
includes client, dedicated calendar ID and age policy, so changing those cannot
relabel a host cache. Android atomically stores a bounded 64 KiB snapshot in
app-private `ambient/calendar.json`, scoped to the host address. An unavailable
host response does not erase its last good cache. Change host address to clear a
different host's snapshot. An age-policy change reaches TV with the next valid
refresh; retained offline snapshots can still contain previously enabled ages.

## Provider choice and connection still required

The personal Outlook connection mentioned in the private brief is not a runtime
credential for this application. No account, calendar or event was changed.
No family dates or credentials are included in source, tests or previews.

The maintained [MS365 Calendar integration](https://rogerselwyn.github.io/MS365-Calendar/)
was evaluated first. Its
[range coordinator](https://github.com/RogerSelwyn/MS365-Calendar/blob/main/custom_components/ms365_calendar/integration/coordinator_integration.py)
can fall back to cached results after an out-of-range Graph request fails.
The normal HA REST calendar response does not prove that the full requested range
was refreshed. That cannot establish this feature's empty/error distinction.
The candidate therefore uses the brief's narrow direct-Graph fallback. It does
not install or reconfigure HA, and does not use an anonymous ICS publication.

[Graph calendarView](https://learn.microsoft.com/en-us/graph/api/calendar-list-calendarview?view=graph-rest-1.0)
supports delegated `Calendars.ReadBasic` for personal accounts and returns expanded
occurrences/exceptions. The adapter only GETs
`https://graph.microsoft.com/v1.0/me/calendars/{configured-id}/calendarView`.
It follows next links only on that exact HTTPS origin and calendar route, rejects
redirects, loops, more than ten pages, over 256 records, over 256 KiB per page or
over 60,000 bytes sanitised. A later-page error, permission failure, invalid row
or disk failure leaves the prior snapshot unchanged. All-day response dates must
be midnight in Sydney / `AUS Eastern Standard Time`; mismatches fail rather than
silently shifting a birthday. Logs contain a fixed failure message only.

The [personal-account device flow](https://learn.microsoft.com/en-us/entra/identity-platform/v2-oauth2-device-code)
helper and refresh adapter use the fixed Microsoft `consumers` authority. The
runtime never creates or edits events. Access tokens stay in memory; only the
rotating refresh token and client ID are atomically stored in a private file
(mode 0600 on POSIX, newly created directory 0700). A refresh commits its rotated
token before returning access. Concurrent callers share one renewal. Windows
operators must protect the directory with a user-only ACL; POSIX permissions
cannot enforce Windows ACLs. Do not put tokens in add-on options, Git, chat or
command arguments. Missing/revoked access leaves the display cache intact.

Remaining setup, in order:

1. In personal Outlook, create the separate occasions calendar described in the
   private brief. Verify its owner and name; obtain its calendar ID through the
   signed-in calendar metadata view or an authorised read-only Graph calendar
   listing. Never select a work/client calendar or the built-in Birthday calendar.
2. Create an Entra application registration supporting personal Microsoft
   accounts and enable public client/device flows. Use delegated
   `Calendars.ReadBasic` and `offline_access`; no client secret, write permission
   or application-wide unattended permission is needed for the display.
3. On a private Node 22+ workstation/host checkout, set `OUTLOOK_CLIENT_ID` to the
   registration ID and `OUTLOOK_TOKEN_FILE` to an **absolute path outside source**
   in a private directory. Run `node tools/connect-outlook-calendar.mjs` manually
   when ready to consent. The helper displays only a short-lived user code.
   Complete sign-in at Microsoft's device-login page with the verified personal
   account. It writes the refresh token without displaying it. This helper has
   been tested with synthetic responses only; real consent is outstanding.
4. Create the ten agreed all-day annual series from the private brief in Outlook.
   Do not copy real dates into repository fixtures. Use short titles, next-day
   exclusive end, Sydney timezone, Free availability, no attendees. Fixed dates
   use yearly month/day recurrence; Australian Mother's/Father's Day use relative
   yearly recurrence. Exclude unconfirmed people and anniversaries. Keep a private
   series-ID inventory: match an existing series before creating, report user
   edits, and never blindly re-run creation. Calendar creation/seeding is not part
   of this read-only application and has not been performed. Sharing invitations
   and reminder delivery remain separate undecided actions.
5. Set `OUTLOOK_CALENDAR_ID` for standalone operation, or `outlook_calendar_id` in
   the proposed add-on configuration; similarly `OUTLOOK_CLIENT_ID` or
   `outlook_client_id`. Blank calendar ID leaves the feed unavailable and makes no
   Graph requests. Keep the normal weather/notice HA configuration unchanged.
   The proposed add-on expects its private token at `/data/cache/outlook-token.json`;
   standalone operation can override this with `OUTLOOK_TOKEN_FILE`. Secure token
   provisioning to that runtime path requires the separately approved host trial.
6. Keep `calendar_ages: false` (standalone `CALENDAR_AGES` unset). For optional ages,
   separately consent with `OUTLOOK_READ_DESCRIPTIONS=true`, requesting delegated
   `Calendars.Read`, and then enable `calendar_ages` / `CALENDAR_AGES=true`.
   A birthday's private plain-text description may contain one exact metadata line
   `ambient:birth-year=2000` (synthetic example). Only the calculated age is emitted.
   Missing/inaccessible metadata never causes an inferred age. Repeat consent
   after revocation; do not increase permissions merely to repair a basic feed.
7. Before adopting the provider on Pi, compare its full 400-day result with
   Outlook: expanded recurrence, exceptions/cancellation, same-day events, Sydney
   all-day boundaries, pagination, edits/deletes, token renewal and failure recovery.
   Do this privately; record counts/results and revision, never family payloads.

## Local validation and preview

- `npm.cmd test`: **51 tests pass**, including Graph pagination/security bounds,
  calendar date/cache/error cases, OAuth renewal and device-flow cases, and all
  existing photo/weather/notice regressions. All dates and tokens are synthetic.
- `tools/build-android.ps1 -Tools EXISTING_PRIVATE_TOOLCHAIN -Unsigned`: **407 JVM
  assertions pass** (15 policy, 208 photo, 25 weather, 30 notices, 129 calendar).
  Android resource/Java/DEX compilation and APK alignment pass without opening or
  using signing keys. Existing Java 8/deprecated fullscreen API warnings remain.
- `node tools/package-addon.mjs`, packaged calendar/auth syntax and host import
  checks pass. The package contains both new runtime modules.
- [Interactive synthetic preview](docs/calendar-comparison.html): normal and
  combined stale/long-weather/narrow/long-name cases rendered and visually checked
  in headless Edge. Zero/one/two/many notices retain space, overflow remains visible,
  and calendar collapses where needed. Empty/unavailable toggles are included.
  This uses browser font metrics, not Android graphics or a device screenshot.

![Synthetic calendar/sidebar comparison](docs/calendar-preview.png)

Outputs are ignored: `build/android/wright-ambient-unsigned.apk`,
`build/bravia_ambient`, and `build/calendar-preview`. The unsigned APK must be
signed with the existing identity under deployment approval before installation.
The JVM checks cover the production map parser and presentation policy, not
Android's JSON bridge, AtomicFile storage or actual lifecycle cancellation.

## Required device acceptance and rollback

Keep the draft PR open. Local/CI success is not device acceptance. Obtain explicit
Pi/TV trial approval for the exact feature-branch revision before device actions.
Preserve the installed known-good host package/APK, private caches and settings;
verify drift and hashes. Build from that revision and record SHA/artifact hashes
in the trial record, without including tokens or snapshots in Git.

Outstanding acceptance:

- Actual personal Outlook consent, renewal after restart and authenticated
  Graph → host → TV path; successful empty versus failed/revoked access, failed
  later pages, persistence/restart and recovery.
- Sony sidebar at couch distance with long names/weather, zero/one/many notices,
  important notices, overflow and stale/unavailable wording. No clipped content,
  lost notice capacity or changed photo bounds.
- Sydney midnight/DST/year rollover and resume while offline; past dates disappear,
  countdown changes, freshness warning appears at 24 hours.
- Whole-photo/portrait pairs, transitions, saved continuation, weather freshness,
  notice changes/expiry and offline retention remain correct.
- Physical first-input/Back dismissal, ordinary idle entry/resume, streaming/HDMI
  behaviour and exit request cancellation. No wake, focus, acknowledgement or edit
  mode is added. Existing input-handler source is unchanged.

For rollback under separate approval, restore the preserved host package and
signed APK from before this calendar trial, preserving existing settings/photo,
weather and notice caches. The older runtime ignores calendar files/options;
remove/revoke the calendar token separately if retiring the connection. Never
restore a refresh token from an old backup over a newer rotated credential.
No deployment, restart, reload, HA change or merge was performed for this work.
