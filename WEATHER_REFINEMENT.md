# Weather summary refinement

## Status and boundaries - 8 November 2026

Implemented locally. The user approved Git publication, the accepted Phase 2
merges and a TV trial on 8 November. The named photo-host update/rebuild was subsequently approved and completed,
followed by the initial TV installation. The user accepted the final display and Back dismissal. ADB was disabled and its
network port refused connections; the user then confirmed ordinary idle entry and
Back both work with debugging off. Relevant trial acceptance and closeout are
complete; separate weather-PR merge approval remains pending. The user accepted the minimalist second mockup,
then requested implementation and clarified a preference for today's forecast
rather than an unavailable current-condition message. Phase 2 is the main-branch baseline; the accepted weather/playback trial is
installed and awaiting its separate merge. The separately approved Phase 2 merges completed: Bravia PR #3 as
6076abeda6acfb7e8fb6f11418917c1faf05d944, companion PR #6 as
729e9f98c528c10a88f836c75d42442ed1d0045d. This refinement does not reopen the notice or aircraft phases.

Keep implementation and tests in Bravia, household dependencies/deployment notes
in the HA companion. Local work is on feature/weather-summary and docs/weather-summary, branched from
the accepted-but-unmerged Phase 2 feature heads to preserve the existing local
notes without merging or changing main. The accepted Phase 2 main merges were incorporated before publication, so
the refinement PRs show only their own changes. Do not publish
new runtime work as an amendment to the already-accepted photo trial.

## Evidence and the correction to current-condition wording

Read-only inspection of the installed BOM weather.py and conditions.py on
8 November confirms:

- Temperature comes from observations_data.data.temp.
- Daily forecasts expose date, condition, maximum and minimum. HA normalises these
  as datetime, condition, temperature and templow through weather.get_forecasts.
- The entity condition prefers today's daily forecast icon, falling back to a
  current-hour forecast when available. It is not an observed weather description.
  The condition_source attribute exposes daily/hourly/unavailable provenance.
- The weather entity exposes no independent observation timestamp or forecast
  issue timestamp. HA last_reported/last_updated prove reporting, not station age.

Earlier explanations described the displayed condition as current conditions.
That was too strong for this installed adapter. Never relabel its daily forecast
as an observation. The prior authenticated Phase 1 check verified daily response
rows for today and tomorrow; this new inspection verifies installed mapping,
not a fresh authenticated forecast response after the outage.

Reference: [HA weather interface](https://www.home-assistant.io/integrations/weather/).
Provider source and household entity mapping remain in the companion notes.

## Visible result

Preserve cream colours, current clock/temperature/date/forecast font sizes, 72/28
photo/sidebar split, photo grouping, complete-image fitting and 60-second runtime
preference. Move information upward rather than increasing text density or size.
Keep the divider at 66% screen height. All weather/status content stays above it;
the entire region below stays blank for future filtered HA notices.

- NOW: observed temperature, with no unavailable-condition message. Following user
  clarification, use today's forecast icon/description under TODAY rather than
  add an observed-condition source. No new HA sensor or provider is required.
- TODAY: daily forecast condition, high and low, explicitly labelled.
- TOMORROW: next local calendar day's condition, high and low, explicitly labelled.
- One small status line above the divider for freshness/partial failure. No
  provider credit line, forecast cards, extra dividers or placeholder notices.

Use HA's actual condition vocabulary, e.g. Rainy or Partly cloudy. The mockup's
Rain easing text was illustrative, not a claim that the standard forecast response
contains narrative wording. Do not invent a forecast description from the icon.
Long conditions wrap into a bounded second line or use a tested short label;
never spill into the notice area or shrink all existing text to fit one phrase.

## Host/feed change

Keep the existing weather entity selection, five-minute refresh, authentication,
fixed weather-only HA operations and provider isolation. The same single daily
forecast request supplies both days; no direct BOM client or extra integration.

Add a bounded dailyForecasts collection containing up to two date-keyed records:
date, condition, high, low and checkedAt. Retain existing temperature/unit and
legacy today's high/low/forecastDay/forecastAt fields during the rollout so the
accepted APK remains compatible. Carry conditionSource and haReportedAt separately;
do not imply that legacy observedAt is an independent station timestamp.

Select today and tomorrow by explicit local date in the configured timezone.
Tomorrow is calendar-date addition, not now plus 24 elapsed hours; cover Sydney
DST start/end and year/month boundaries. Continue handling BOM's offset-free
local-midnight rows and valid offset-bearing timestamps without depending on the
Node process timezone. Do not substitute the next available row when a target
calendar date is absent.

Retain validated last-good forecast records only for the same provider and target
date. Store their own successful check timestamps. A failed request or missing row
must not advance a retained record's checkedAt. Do not mix fields across providers
or dates. At midnight an existing tomorrow record can become today only because
its stored date matches, with its original age preserved. An absent new tomorrow
stays unavailable. Never display yesterday under Today.

## Honest freshness and failure behaviour

Use the term Checked, not Observed or Issued, for host retrieval times. They do
not prove upstream freshness. Do not add unverified observation/issue timestamps
or a new BOM API dependency just to match the first mockup.

- Successful current and forecast checks: Checked N min ago, based conservatively
  on the oldest successful check among the data actually shown.
- Mixed successful/current and retained forecast data: explicitly say Forecast
  cached, with its age; do not let a fresh temperature hide a failed forecast.
- No valid timestamp: Check time unavailable. A future timestamp/clock anomaly
  must not produce a negative age or an unwarranted fresh state.
- Missing one day: keep the other valid section and show Forecast unavailable in
  the missing section. The temperature does not have to disappear with forecasts.
- Continue the existing stale thresholds initially: current retrieval 30 minutes,
  HA report 3 hours, forecast check 6 hours. Treat these as application cache checks,
  not guarantees about station/forecast source freshness. Document them explicitly.
- The TV recomputes dates/age each minute even when no feed refresh succeeds.

Last-good weather remains supported. Photo caching, source exports, credentials,
attribution retained in the feed, and weather provider selection are unchanged.

## Implementation and validation sequence

1. Review this scope and close the accepted photo PRs through their separate merge
   approval. Start the focused branches without losing local notes.
2. Implement additive host schema/date/cache logic, then the Android view-model
   and minimal sidebar layout. Use synthetic weather for local verification.
3. Cover both days, offset-free/offset-bearing timestamps, DST/calendar changes,
   missing/malformed/duplicate rows, partial field availability, provider changes,
   failed retrieval with retained age, midnight rollover, legacy cache/feed input,
   unknown condition and long text. Assert the notice region remains untouched.
4. Run host tests, package/syntax checks, meaningful JVM weather-model cases, full
   Android build/signature check and visual layout review. Review the diff; do not
   repeat unrelated device tests solely because a local build passed.
5. Obtain scoped commit/push/PR approval as required by AGENTS.md. Review exact
   branch revisions and CI before requesting named host/TV trial approval.
6. Before any live write, verify the selected daily response again through an
   authenticated approved session without printing credentials; preserve the actual
   installed APK/package and private settings backup. An unauthenticated 401 is
   not a weather result. No HA Core restart is needed for this design.
7. Trial the additive host first against the existing app; then install the new
   APK preserving preferences/cache. Record exact revisions and artifact hashes.
   Verify real Today/Tomorrow labels/values, readability at viewing distance,
   long/missing/cached states, reserved notice space, normal Back/idle and any
   separately approved failure/recovery exercise. Do not simulate a power cut.
8. Obtain user acceptance, close debugging, verify ordinary use, document outcomes
   and limitations, then request separate merge approval. There is no automatic
   implementation of notice sources, phone migration or aircraft after this work.

## Recovery

Keep both current and previous known-good signed APKs and host packages privately.
The additive feed permits app-first rollback to the accepted Phase 2 APK without
clearing data. Host rollback uses its preserved package/backup and compatible
cache handling; verify old code ignores new optional fields before relying on it.
Do not uninstall, clear photos/cache, inspect credentials or alter shared toolchains.

## Not part of this change

Live notices/remote interactions, Thread health rules, SSH startup policy, automatic
repairs/watchdogs, extra phone routes, independent HA outage monitoring, aircraft,
photo ordering/shuffle, photo cache removal and system-package debloating
remain separate. The reported power-reset scenario is retained in ROADMAP.md as
future dependency-health evidence; it does not authorise changing startup settings.

## Local implementation evidence

- Host now provides up to two explicit date-keyed dailyForecasts, retaining legacy
  today fields for the accepted APK. HA condition-source/reporting metadata stays
  separate from observations. A forecast description is never inferred to be an
  observed current condition.
- Failed/missing forecast rows keep original check ages for the same provider/date;
  date rollover selects matching records and missing days stay explicit. Invalid
  calendar dates, future timestamps and unknown conditions cannot look fresh.
- Android renders Now temperature and compact Today/Tomorrow condition/icon/range
  sections, with freshness above the existing divider. Current typography sizes,
  cream palette, grouping and 60-second preference are preserved. Below-divider
  space remains empty. The new view handles legacy host/cache ranges safely.
- 22 synthetic Node tests passed; package generation and packaged-server syntax
  check passed. Full local Android build/signature verification passed with
  15 existing policy + 200 photo + 17 new weather assertions (232 total).
- Initial local APK SHA-256: 0318e386b6e8cf61c99bbe77ad1bb3f5d3e61156b4a7b43c140802f314121779.
  This identifies the initial artifact; later installed revisions are recorded in
  the private trial inventory.
- The initial exact-head CI and named host/TV trial passed their build/install
  gates. The new weather fields rendered using the TV's configured authenticated
  connection. Final user acceptance and testing-session closeout remain pending.
  No HA Core restart, provider/option or credential change occurred.

## Trial progress and requested adjustments

The user approved Git publication, accepted Phase 2 merges, the TV trial and
separately the named photo-host update/rebuild. Initial weather PR heads passed CI
and the host/TV trial was deployed from the exact weather feature revision.
Source identity and trial evidence are retained locally; private recovery artifacts
and inventory remain outside version control. Health/authentication checks passed.
The TV displayed both days and retained pairing/60-second settings. User visual
acceptance and debugging closeout are pending. No HA Core restart occurred.

The user rejected Cached weather wording and asked to continue the photo loop
across sessions. The revised candidate uses check-age wording with specific
delayed-check warnings. Read-only HA template checks confirmed recent reports;
the original diagnostic draft was restored and HA configuration was unchanged.
The initial freshness warning was traced using numeric age/flag logs only,
with no credentials, private addresses or photo IDs logged.

Playback now saves the next photo and last shown photo in private preferences only
when a scene starts displaying. Both portraits count as shown. Static order and
end-of-album wrapping remain; a missing next photo falls back after the last shown
photo where possible. Preview and actual dreams share this state. First use begins
once at the start because old versions recorded no cursor. Preference writes are
asynchronous, so the latest scene across sudden power loss is not guaranteed.
No photo/cache deletion or credential change is involved.

Expanded local validation passes: 15 policy + 208 photo + 17 weather assertions
(240 total), signed Android build; 22 host tests remain passed for unchanged host
code. The revised exact-commit candidate subsequently passed the checks below.

## Final candidate verification and closeout

Runtime revision 55a749b52a065400bb489e29020b9cf658f43431 passed CI and was
installed on the TV. The actual APK was pulled back and matched the local signed
artifact; its hash and recovery inventory are stored privately outside Git. The
host remains on the earlier reviewed runtime revision because this correction
changes Android only; no additional host rebuild was required.

The first device trial exposed an Android timestamp-parser mismatch: host freshness
was false for stale, but TV parsing returned unknown report age. HA live/serialized
reporting timestamps and device clocks were checked read-only. Parsing offset-aware
ISO date-times fixed the problem on the Sony; healthy data now shows only Checked
N min ago. Four added timestamp-format cases cover microsecond offsets and host Z
timestamps. Local validation now totals 244 JVM assertions; the unchanged host's
22 tests remain passed. Build/signature and runtime-source CI passed.

Private screenshots confirmed full photos, separate Today/Tomorrow descriptions
and icons, a quiet check-age line and empty space below the divider. Photos
continued beyond the initial group after Preview exit, an explicit app process
restart, and a subsequent APK replacement. Pairing and the 60-second preference
remained set. This establishes process/session continuation, not a guarantee that
the newest asynchronous preference write survives an abrupt electrical outage.

The user accepted the final couch-distance display and physical Back dismissal.
ADB was then disabled; TCP 5555 returned ECONNREFUSED. The user subsequently
confirmed normal idle entry and Back with debugging off. Developer options master
remains enabled to preserve unreviewed settings; reboot persistence is untested.
Both weather PRs stay open awaiting separate merge approval. Failure simulation,
real midnight/DST rollover, HDMI/streaming/power and long-run performance remain
unverified. No HA Core restart or unrelated live change was performed.
