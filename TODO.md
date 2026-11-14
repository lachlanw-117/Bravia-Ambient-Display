# TODO

Updated 15 November 2026 (Australia/Sydney). **Calendar work is parked at the
user’s request until more physical tasks on the to-do list have progressed.**
There is no scheduled or automatic resumption. Do not start calendar setup,
deployment or merge merely because local/CI checks passed.

Read [AGENTS.md](AGENTS.md) before acting. Checked items describe recorded results,
not a fresh live inspection. This checkpoint supersedes older planning status in
the [roadmap](ROADMAP.md) and dated trial records.

## Physical / device follow-up

These remain open checks; this list does not authorise device actions or prescribe
the order of the user’s wider household physical tasks.

- [ ] Complete the [11 November TV acceptance checks](TV_TEST_2026-11-11.md): Home
  idle timing, streaming, paused-video and Xbox/HDMI results individually.
  Earlier scoped ordinary idle/Back acceptance is not full coverage.
- [ ] Determine whether this Sony firmware exposes a configurable idle timeout.
- [ ] Complete longer-run photo fitting/orientation, portrait pairs, crossfades,
  continuation and offline/outage recovery checks with the exported album.
- [ ] Observe actual Sydney midnight/DST and forecast date rollover on the TV.
- [ ] Verify a same-date/provider Today minimum is captured, then retained after
  blank responses. Synthetic success and an expected blank without a captured low
  do not establish this lifecycle; see [layout trial](HOME_STYLE_REVIEW.md).
- [ ] Review debugging closeout at the next separately approved device session.
  Earlier trials disabled ADB; the 14 November notice trial explicitly left it
  enabled at the user’s request. Do not infer current state or change it without
  approval. See [notice trial](HOUSEHOLD_ALERTS.md).

## Household Attention — trial incomplete

- [x] Implement the display-only TV-filtered feed and independent host/TV cache.
- [x] Trial-deploy with approval; correct HA’s textual schema-version mismatch.
- [x] Record user confirmation that both natural notices display correctly.
- [ ] Complete broader lifecycle/routing, grouping, expiry, real-TV overflow,
  restart, outage/recovery and physical remote/idle acceptance.
- [ ] Obtain separate merge approval after relevant acceptance is recorded.

[PR #8](https://github.com/lachlanw-117/Bravia-Ambient-Display/pull/8) is a separate
unmerged trial in the last recorded state. Its branch was unchanged by calendar
work. [HOUSEHOLD_ALERTS.md](HOUSEHOLD_ALERTS.md) owns installed identities, rollback
and acceptance limits. Recheck PR/device state before resuming.

## Calendar — implemented and tested, parked before setup/trial

- [x] Build read-only Outlook retrieval, bounded pagination, authenticated
  /calendar, independent persistence, freshness and offline date handling.
- [x] Apply the accepted quieter sidebar: Home Assistant with up to five measured
  messages when notices exist; otherwise Coming up. Carousel remains unbuilt.
- [x] Align ownership: private wright-family-calendar owns source dates, nicknames,
  milestone calculations, approved subjects, ten-year publishing, maintenance and
  selected work/client copies. Bravia displays subjects verbatim, without age or
  anniversary arithmetic, nickname replacement, calendar writes or phone pushes.
- [x] Support multi-day all-day events through the last included Sydney date, with
  correct ranges, Underway status and exclusive-end expiry offline.
- [x] Pass 53 synthetic host tests, 343 JVM assertions, unsigned Android build,
  package/import checks and exact-runtime-head CI; inspect synthetic preview.
- [x] Record the interface, read-only authentication setup, rollback and outstanding
  device checks in [CALENDAR.md](CALENDAR.md).
- [ ] **Parked:** coordinate the publisher-owned calendar ID and verified personal
  owner with wright-family-calendar. Do not recreate publication work in Bravia.
- [ ] **Parked:** configure/consent/provision delegated Calendars.ReadBasic plus
  offline_access and private refresh-token storage within the display integration.
- [ ] **Parked:** obtain explicit exact-revision Pi/TV trial approval and complete
  real reading/renewal, Sony layout, midnight/multi-day/offline and regression checks.
- [ ] **Parked:** record relevant device acceptance before requesting separate
  merge approval. Keep the draft PR open; nothing calendar-related is deployed.

Resume checkpoint:

- Branch: feature/calendar-sidebar; draft [PR #10](https://github.com/lachlanw-117/Bravia-Ambient-Display/pull/10).
  Stacked on feature/household-alerts at
  0c7ce4518c0857fa9d4f1d24ad61925bb5f89d55. Do not modify/merge PR #8 as a shortcut.
  The calendar candidate is not on main.
- Last tested runtime: abfed948bdfa332e99a7dd5d5b03bc5d2308829a.
  Successful [CI run 43653319559](https://github.com/lachlanw-117/Bravia-Ambient-Display/actions/runs/43653319559).
  Later documentation-only commits do not change this tested runtime identity.
- [Interface/setup/acceptance](CALENDAR.md), [interactive preview](docs/calendar-comparison.html)
  and [rendered preview](docs/calendar-preview.png). Schema v2 discards older
  candidate caches that may contain transformed subjects; other caches stay separate.
- No real calendar consent/seeding, Pi/TV deployment, reload, restart or merge
  occurred in the calendar session. Current live device state is unverified.
- Preserve unrelated untracked HOME_SCREEN_ADS.md. Photos, real source dates,
  credentials, private backups and signing keys remain outside Git.

To resume in a new chat, use:

> Resume the parked Bravia calendar work. Read AGENTS.md, TODO.md and CALENDAR.md,
> then inspect feature/calendar-sidebar, draft PR #10 and its PR #8 dependency.
> Confirm current branch/PR status and the publisher interface before changing
> anything. Do not deploy, operate devices or merge without explicit approval.

The repo and PR are the handoff; the original chat and ignored build outputs are
not required. Rebuild from recorded source if artifacts are missing. Use current
credentials only through the agreed integration; never copy secret contents into
chat. Real publication data belongs in the private calendar repo and is absent here.

## Other deferred work

- [ ] Review targeted Home-screen advertising/launcher options separately;
  in-streaming-app ads and blanket debloating remain excluded.
- [ ] Plan aircraft work separately before implementation.
- [ ] Revisit photo metadata or Google Ambient API partner access only if needed.
- [ ] Remote notice navigation, hiding and acknowledgement remain deferred;
  every input continues to dismiss the current screensaver candidate.

## Recorded completed baseline

- [x] Research Google Photos restrictions and Android DreamService lifecycle.
- [x] Build screensaver, local host and bounded offline photo/weather cache;
  establish host/JVM checks and Android packaging.
- [x] Deploy the initial 147-photo album with approval; see [TRIAL.md](TRIAL.md).
- [x] Complete Phase 0 roadmap, decisions and maintenance documentation.
- [x] Complete scoped BOM weather trial/closeout and approved PR #2 merge;
  see [PHASE1.md](PHASE1.md).
- [x] Accept cream palette, whole-photo/portrait pairs, 60-second scenes and scoped
  ordinary idle/Back; complete approved Phase 2 merges. See [PHASE2.md](PHASE2.md).
- [x] Accept and merge weather-summary/photo-continuation work (PR #4);
  see [WEATHER_REFINEMENT.md](WEATHER_REFINEMENT.md).
- [x] Implement/trial the home-style/date/weather layout and dated-low retention;
  record scoped acceptance and the subsequently approved PR #5 merge. Detailed
  low-retention lifecycle and wider device checks remain open above.
