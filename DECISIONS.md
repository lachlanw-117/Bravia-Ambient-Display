# Bravia decisions and deferred work

Decisions recorded on 8 November 2026. Read the [roadmap](ROADMAP.md) for phase
boundaries and status. An accepted design is not an implemented or deployed
feature. [TRIAL.md](TRIAL.md) records historical device results.

For a new decision, record its status/date, phase, reason, alternatives, evidence,
uncertainty and revisit condition. Link implementation revisions and deployment
results when they exist; until then state **not implemented**. Preserve a
superseded decision and link its replacement rather than erasing the reason.

## D01 - Do not apply skinny-bravia

**Status:** accepted exclusion; all phases. No cleanup from that project applied
by this work.

The [guide](https://github.com/therealhoodboy/skinny-bravia) targets Android 9 and
includes broad removal/disablement of packages relevant to HDMI/Bravia Sync,
screensavers, rendering and accessibility. Our recorded TV runs Android 12 and
already plays photos. There is no measured app bottleneck that justifies the
blanket package changes. An uninstall list is not a performance diagnosis.

Alternative: measure our app's decoding, rendering, memory and input handling
first. Revisit only for a demonstrated bottleneck, an individually understood
package, Sony-version compatibility, explicit device approval and a rollback.

## D02 - Use native HA for the notice foundation

**Status:** accepted design; Phases 3, 5 and 6; not implemented.

Use core-maintained helpers, scripts, automations and source events with a small
feed adapter. Provide everyday destination controls in HA rather than requiring
YAML edits for each preference change. This matches the household's practice of
making quick adjustments in the UI. Restored helper values are runtime state;
source logic and definitions remain reviewed configuration in Git.

[Ticker](https://github.com/analytix-energy-solutions/ticker) is a close feature
match for subscriptions and history, but research found a younger project,
concentrated maintenance, documented auto-clear restart limitations and open
reliability/permission reports. An issue report is not independently verified
proof. AI assistance alone is not a reason to reject software.
[Alert2](https://github.com/redstone99/hass-alert2) has a longer project history
and useful suppression/snooze features, but still needs feed/routing work and
adds another dependency. Neither is selected as a dependency by this plan.

Revisit if native configuration becomes difficult to maintain, an established
manager demonstrably reduces custom work, or more advanced fault lifecycles are
needed. Compare actual release behaviour, tests, permissions and maintenance,
not just feature lists or popularity. Do not build a full custom manager by
default.

## D03 - Designs B and C, with adaptive portrait pairs

**Status:** Phase 2 trial-deployed with scoped visual/ordinary-use acceptance recorded;
future Phase 7 remains an accepted concept only. See [Phase 2](PHASE2.md).

B is a photo area beside a clear information column. C is a dark aircraft map
with selected-flight details and the shared notice layer. The generated mockups
used sample content; they were concepts rather than app screenshots. B suits
the selected everyday layout; C remains a separate feature.

Many album photos are portrait and currently leave black space on both sides.
Pair consecutive portraits when they fit, preserve complete foreground images,
retain album order and provide a single-photo setting. Automatic cropping was
not selected. Use subdued static fill for residual space. Revisit pairing or
information-column proportions after couch-distance review and actual TV tests.

## D04 - Persistent offline storage is no longer required

**Status:** accepted requirement change; cache removal deferred.

The existing host/TV caches remain implemented and were unchanged by the album
deployment. Removing them is not part of visual work or project cleanup. A
replacement still needs bounded downloads/decoding and defined failure behaviour.
Revisit only through a separate storage discussion, with migration and recovery
implications reviewed. Do not clear app data to make implementation match the
new requirement.

## D05 - Notices are passive outside the running screensaver

**Status:** accepted design; Phases 3-6; not implemented.

Meaningful household occurrences and attention conditions enter a typed feed,
then destination filters decide delivery. Record before routing so suppressed
items can be reviewed privately. Do not copy every state transition or raw log.
TV notices must not wake the TV, launch over playback or switch HDMI inputs.

Updates are grouped from their first implementation; see the
[count and lifecycle rules](ROADMAP.md#updates-are-grouped-from-the-first-implementation).
Individual component/version details belong in the private inbox, with one
TV-facing count. Revisit source breadth only if a specific useful event is
missing; do not respond to noise by adding more unfiltered sources.

## D06 - Include remote interaction and local TV hiding

**Status:** accepted design; Phase 4; not implemented.

The user wants both HA source integrations and TV remote interactions. Hide only
the selected occurrence on this TV, persist the hidden state, allow it to be
revealed in setup, and let new occurrences reappear. Back exits immediately;
Home/power remain system-owned. Preserve a first-input-exit setting.

Local hiding is smaller and clearer than shared acknowledgement. It does not
resolve a fault, dismiss an HA Repair or silence phone reminders. Revisit shared
acknowledgement only with separately designed permissions, state synchronisation
and failure semantics. Sony input behaviour remains unverified for this feature.

## D07 - Prefer the installed BOM weather source

**Status:** Phase 1 deployed, scoped acceptance/closeout verified and PR #2 merged on 8 November.

Read-only inspection on 8 November identified the installed BOM component and
its daily/hourly entities; household identifiers belong in the
[HA companion notes](https://github.com/lachlanw-117/Wright-Home-Assistant/blob/main/docs/bravia_ambient_host.md).
The host already supports a selected HA weather entity and daily forecasts.
Prefer that interface over adding a separate direct BOM client to the TV.

Keep provider attribution in the feed/docs, and make source freshness and local-day
correctness clear. During the 8 November live trial the user rejected the full
sentence, then a smaller credit, and explicitly selected no visible TV credit.
That supersedes the original visible-attribution requirement; retain the reason
so a later redesign does not reintroduce it by accident. Do not
silently fall back to another provider. Entity ownership was inspected, but live
readings/forecast output were not verified because the attempted Supervisor API
route returned 401. Subsequent authenticated browser inspection verified current
readings and daily forecast output on 8 November. The offset-free forecast dates
need explicit local-calendar handling; that fix and provider-isolated caching are
deployed. Attribution remains in the feed, with visible credit removed after user
review. See [Phase 1](PHASE1.md) for evidence, acceptance and freshness limitations.
Revisit if upstream observation
age cannot be established sufficiently for household use, or actual responses
change. Future deployments still require a separately approved source change.

## D08 - Aircraft details are conditional, not a promised free data service

**Status:** accepted research direction; Phase 7 planning deferred.

One anonymous Sydney-region sample found usable positions/type data from
[adsb.fi](https://github.com/adsbfi/opendata) and
[adsb.lol](https://www.adsb.lol/docs/open-data/api). This is not a coverage or
reliability guarantee. ADSBDB enrichment returned useful details in the sample,
but its [route-data notice](https://www.adsbdb.com/) requires permission/use
clarification. Callsign matching is not independently verified live routing.
Aircraft data and map imagery are separate provider decisions.

Revisit with sustained coverage/age checks, permitted display/cache use, map
attribution and a budget decision before implementing C. No paid subscription,
receiver purchase, website scraping or precise home location in Git is selected.

## D09 - Close maintenance sessions instead of leaving debugging as a dependency

**Status:** accepted procedure; no device cleanup performed in Phase 0.

Normal app operation does not require ADB. After an approved device session,
disable debugging and verify ordinary use; review the Developer options master
switch separately. Disconnecting the computer does not disable TV debugging.
Last recorded debugging was enabled; its present state is not assumed.

Keep recovery artifacts and signing material private. Do not equate cleanup with
deleting caches, backups or source photos, removing system packages, disabling HA
Terminal & SSH, or relocating shared toolchains. Follow
[MAINTENANCE.md](MAINTENANCE.md). Revisit when an actual maintenance need appears.

## D10 - Accepted main branches and short-lived changes

**Status:** repository consolidation authorised on 8 November 2026.

Use `main` as the accepted baseline in each repository, with a focused branch/PR
per coherent change. GitHub Actions runs synthetic/local checks; it does not
deploy. Deployment records identify installed revisions separately from `main`.
Retire incorporated branches without rewriting history. Preserve superseded work
with an archive reference before deleting its branch. See
[CONTRIBUTING.md](CONTRIBUTING.md).

On 8 November the user refined the acceptance order: keep runtime PRs open,
trial-deploy the exact feature-branch commit with approval, record successful
relevant device acceptance and closeout, then request merge approval. This
supersedes merging runtime changes before deployment. CI cannot establish Sony
rendering/input or physical HA behaviour. Failed trials stay on the branch for
fixes and retesting. Documentation-only changes need review/local checks without
device deployment. Historical baseline merges do not become retroactive claims
of device acceptance. Revisit only if a suitable isolated staging environment can
establish the same acceptance evidence without the household trial.

The initial Bravia branch was also its default branch. Consolidation establishes
`main` without discarding that history. HA keeps its existing `main`. The repos
retain separate source/household responsibilities. Bravia's private visibility is
unchanged; publishing is a separate decision. Revisit branch complexity only if
parallel release support or collaboration creates a demonstrated need.

## D11 - Display-only TV notices first

**Status:** accepted and authorised for local implementation, 14 November 2026;
subsequently published and deployed for trial with approval. The HA schema type
mismatch is corrected; TV acceptance remains pending. PR #8 is open and unmerged.

Consume HA's TV-filtered household attention sensor through the existing private
host. HA retains lifecycle and routing ownership. Show a compact severity-ordered
list below weather, up to three readable two-line rows. Overflow is a count with
“View in HA”, not automatic paging. Leave a fresh empty feed quiet. Tighten the
clock-to-Now gap and put the divider directly beneath the weather check text.

Retain first-input dismissal. Navigation, local hiding and acknowledgement are
deferred; this supersedes the earlier Phase 4 requirement to ship interaction
with the feed. During outages retain last-known unresolved notices with a warning
after two minutes; one-shot events still expire at HA's deadline. An unavailable
feed is not proof of recovery or household health. See [the implementation and
acceptance record](HOUSEHOLD_ALERTS.md).
