# Bravia implementation roadmap

Accepted design recorded on 8 November 2026. Phase 0 is implemented as repository
documentation. Phases 1-6 remain planned and require separate authorisation;
Phase 7 requires a later implementation plan. Accepting this roadmap is not
approval to deploy, operate devices or progress automatically to another phase.

Read [AGENTS.md](AGENTS.md) before acting, then this roadmap, the
[decision log](DECISIONS.md), [maintenance procedure](MAINTENANCE.md) and
[historical trial results](TRIAL.md). Read the companion HA repository's
`AGENTS.md` and [household host notes](https://github.com/lachlanw-117/Wright-Home-Assistant/blob/main/docs/bravia_ambient_host.md)
before work that crosses repositories.

## Current baseline and ownership

The last recorded host/TV deployment used source
`3fbb12727382c538e43fdef125de39687d20bc07` on 7 November 2026. The user confirmed
visible playback of the imported 147-photo album. The installed design uses one
fitted photo, a bottom-right weather/clock overlay and first-input dismissal.
It still caches photos. Individual playback, HDMI, input, power and failure
scenarios remain pending; repository records do not establish current live state.

Bravia owns Android and host source, packaging, interfaces and tests. HA owns
household configuration, entity mappings, routing definitions and deployment notes.
Do not duplicate reusable source in HA. Keep photos, precise household location,
runtime state, backups, options and signing material outside Git.

## Phase boundaries and stopping points

| Phase | Deliverable and acceptance gate | Excluded |
|---|---|---|
| **0 - Record the project** | Roadmap, decisions, deferred work and cleanup documented; local links and diff checked. Stop after documentation. | App/configuration changes and device actions |
| **1 - BOM weather** | Use the installed daily BOM entity, verify current readings and today's forecast, and show provider attribution. Review source, freshness, local-day selection and rollback before approved deployment. | Photo redesign and notices |
| **2 - Photo presentation** | Design B, adaptive portrait pairs, complete-image fitting, subdued background and improved typography/weather layout. Check ordering, transitions, memory and normal exit on the TV. | Live notices and aircraft |
| **3 - Notice foundation** | Typed records, private inbox, restored active state and native HA routing controls. Washing, backups and grouped updates start capture-only. Verify lifecycle and restart behaviour. | Replacing phone notifications and showing live TV notices |
| **4 - TV notices and interactions** | Filtered TV feed, remote navigation and local hiding, with immediate exit. Verify Sony input behaviour and persistence. | Household-wide acknowledgement, HA repairs and device control |
| **5 - Phone routing** | Migrate explicitly selected types, washing first and backups next. Verify each recipient independently, no duplicate delivery, and unchanged unrelated actions. | Blanket migration of automations |
| **6 - Broader attention sources** | Add Repairs, persistent notifications and selected device-health rules one family at a time, initially capture-only. | Every entity transition, general log forwarding and automatic repairs |
| **7 - Aircraft feature** | Write a separate implementation plan for design C after resolving flight data, route accuracy, permitted use and map supply. | Aircraft implementation under earlier approvals |

There is no automatic progression. Phase 3 is useful as a private inbox even if
the TV, phone migration or aircraft phases do not proceed. Source families and
phone migrations also have individual review points within their phase.

For each phase: **build locally, review, approve named live changes, verify,
document, close the testing session**. Follow each repository's rules for
commits, pushes and merges; installation approval does not cover those actions.
Do not mark a phase verified because its build passed. Use synthetic test data.

## Accepted display behaviour

Design B is the everyday photo/information layout; design C is the future
aircraft scene. These are selected concepts, not installed features.

For design B, show two consecutive portrait photos when both fit, one landscape
otherwise, and an unmatched portrait alone. Preserve whole foreground images and
album order. Use a subdued static background for residual space, not black bars
as the intended visual treatment. Keep a single-photo option. Advance the visible
group at the configured slide interval.

Decode for the actual display rectangles. Bound decoding/prefetch and retain
only current/incoming groups. Generate background effects once per group rather
than continuously blurring full-resolution images. Stop scene-specific work on
exit. Separate notice/weather refresh from slow album synchronisation. Keep the
existing photo-cache behaviour until a storage change is discussed separately.

## Accepted notice behaviour

The intended flow is **meaningful HA sources -> typed private feed -> destination
rules -> phones and TV**. The raw feed is before destination filtering; it is not
an unrestricted copy of HA's event bus. Routine light changes, sensor churn and
general logs are excluded.

Records retain category, source identity, occurrence identity, severity, concise
text, timestamps and lifecycle information. Repeated reports update an incident.
One-off events expire; ongoing faults clear through explicit recovery conditions.
Unknown data must not silently resolve a fault. Preserve source references for
investigation. A device being off is not itself a fault; sleeping, disabled and
deliberately disconnected devices need appropriate exclusions.

Everyday HA controls provide category switches for each phone and the TV,
destination pauses and a private inbox containing delivery-suppressed items.
Preferences restore after restart and take effect without editing YAML. Git owns
definitions/logic; helper values are runtime preferences, not source drift to
tidy later. HA phone delivery must not require a working TV or photo host.

New sources begin capture-only. Keep existing washing notifications during this
trial. Later migration must remove duplicate delivery while preserving the
laundry-light action. Destination delivery failures must be independent.

### Updates are grouped from the first implementation

- The TV shows one summary such as **4 updates available**, not four cards.
- The private inbox retains individual component/version details.
- Count distinct components confirming an available update. Unknown/unavailable
  states do not count as confirmed updates.
- Installation or skipping changes the existing summary; zero removes it.
- Do not send individual component update notifications to phones.
- A locally hidden summary remains hidden as existing updates are removed.
  Newly available components or versions make it eligible to appear again.

### TV feed and interactions

Plan an authenticated, read-only `/notices` interface exposing only TV-approved,
sanitised records. Do not expose the private inbox or an HA write/control proxy.
Notice display operates only while the screensaver runs; it cannot wake the TV
or interrupt normal viewing.

Both HA feed integration and remote interactions are wanted in Phase 4:

- OK opens notice controls; D-pad selects a notice; OK hides its occurrence.
- Back exits the screensaver immediately; Home and power stay system-owned.
- Setup can reveal locally hidden items.
- A setting retains first-input exit when interactive controls are disabled.
- Local hidden state survives app restart; new occurrences can reappear.

Hiding is not resolution, HA Repair dismissal or phone acknowledgement. Verify
remote behaviour on Sony firmware, including how volume and underlying apps
behave. Desktop tests alone cannot establish these results.

## Handoff and phase records

Use these status labels precisely: **proposed**, **accepted design**,
**authorised implementation**, **implemented**, **deployed**, **verified**.
Implementation, deployment and verification are separate facts. Record the date,
scope, exact source revision/local diff, checks, limitations and rollback at each
transition. A decision's revision is found in Git history; deployed revisions
must be recorded explicitly in trial/household notes.

Phase 0 was authorised and implemented locally on 8 November 2026. No app, HA or
device changes were made. Later phases have accepted designs, not implementation
or deployment authorisation. Commit/push/merge approval was supplied separately
for repository consolidation on 8 November; it does not authorise live changes
or later feature phases. See [CONTRIBUTING.md](CONTRIBUTING.md) for the workflow.
The next possible phase is BOM weather, after its own scoped authorisation.
