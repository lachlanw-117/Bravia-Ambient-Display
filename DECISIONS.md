# Design decisions

Current choices, reviewed 14 November 2026. The [design evolution](docs/design-evolution.md)
explains how household trials shaped them; the [original decision log](docs/history/decisions-2026-11-14.md)
preserves alternatives and superseded proposals.

## Android owns the screensaver lifecycle

DreamService uses Android's idle lifecycle. The app does not wake the TV, switch
inputs or launch notices over viewing. Every remote input dismisses it. Broad
Sony package removal was excluded without a measured bottleneck and a specific,
understood change. See [architecture](docs/architecture.md).

## Fit whole photos and bound resource use

Complete-image fitting and consecutive portrait pairing preserve photographs
and album order. Bounded transactional caches retain working content through
failed refreshes. An earlier discussion relaxed the offline requirement; it did
not remove caches or establish a cache-removal task.

## Reuse HA interfaces and keep ownership clear

The host consumes HA weather and daily forecasts, retaining provider attribution
in the feed. Household source rules, notice recovery, routing and phone delivery
belong in HA. Bravia exposes authenticated, read-only feeds; HA credentials stay
on the host and private runtime values stay outside Git.

## Keep notices passive and compact

The current scope is display-only: up to three severity-ordered rows, grouped
updates, an overflow count and no paging. First-input exit is preserved. Earlier
remote hiding/acknowledgement proposals are superseded, not active commitments.
Failed retrieval retains last-known unresolved items with a stale warning;
events still expire at HA's deadline. See the [notice contract](HOUSEHOLD_ALERTS.md).

## Distinguish source, deployment and acceptance

Accepted main and installed revisions are recorded separately. Runtime changes
need an approved exact-revision trial and scoped acceptance before merge approval;
documentation needs review and local checks. The notice PR stays open and
unmerged by request. ADB is unnecessary for ordinary operation, but the owner's
14 November instruction explicitly leaves it enabled. See [maintenance](MAINTENANCE.md).

## Historical decision references

Earlier decision IDs remain available below. Their original wording and statuses
belong to the dated archive; this page and the [current roadmap](ROADMAP.md) state
the present scope.

<a id="bravia-decisions-and-deferred-work"></a>
<a id="d01---do-not-apply-skinny-bravia"></a>

- [D01: Sony package changes](docs/history/decisions-2026-11-14.md#d01---do-not-apply-skinny-bravia).
<a id="d02---use-native-ha-for-the-notice-foundation"></a>

- [D02: HA notice foundation](docs/history/decisions-2026-11-14.md#d02---use-native-ha-for-the-notice-foundation).
<a id="d03---designs-b-and-c-with-adaptive-portrait-pairs"></a>

- [D03: Layout concepts](docs/history/decisions-2026-11-14.md#d03---designs-b-and-c-with-adaptive-portrait-pairs).
<a id="d04---persistent-offline-storage-is-no-longer-required"></a>

- [D04: Offline requirement discussion](docs/history/decisions-2026-11-14.md#d04---persistent-offline-storage-is-no-longer-required).
<a id="d05---notices-are-passive-outside-the-running-screensaver"></a>

- [D05: Passive notices](docs/history/decisions-2026-11-14.md#d05---notices-are-passive-outside-the-running-screensaver).
<a id="d06---include-remote-interaction-and-local-tv-hiding"></a>

- [D06: Superseded interaction proposal](docs/history/decisions-2026-11-14.md#d06---include-remote-interaction-and-local-tv-hiding).
<a id="d07---prefer-the-installed-bom-weather-source"></a>

- [D07: Weather provider](docs/history/decisions-2026-11-14.md#d07---prefer-the-installed-bom-weather-source).
<a id="d08---aircraft-details-are-conditional-not-a-promised-free-data-service"></a>

- [D08: Aircraft research limits](docs/history/decisions-2026-11-14.md#d08---aircraft-details-are-conditional-not-a-promised-free-data-service).
<a id="d09---close-maintenance-sessions-instead-of-leaving-debugging-as-a-dependency"></a>

- [D09: Original maintenance procedure](docs/history/decisions-2026-11-14.md#d09---close-maintenance-sessions-instead-of-leaving-debugging-as-a-dependency).
<a id="d10---accepted-main-branches-and-short-lived-changes"></a>

- [D10: Source and trial workflow](docs/history/decisions-2026-11-14.md#d10---accepted-main-branches-and-short-lived-changes).
<a id="d11---display-only-tv-notices-first"></a>

- [D11: Display-only notice scope](docs/history/decisions-2026-11-14.md#d11---display-only-tv-notices-first).
