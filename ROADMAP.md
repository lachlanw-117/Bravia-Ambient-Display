# Current product and future direction

Updated 14 November 2026. The photo, weather and household-notice screensaver is
complete for the owner's current needs. There is no active sequence of remaining
photo/notice development phases. See the [documentation index](docs/README.md).

## Current product

The Android screensaver displays whole photos and portrait pairs, remembers album
progress, shows observed temperature and dated forecasts, and presents HA-approved
notices. Android owns idle entry and first-input exit. The private Pi host serves
authenticated read-only feeds.

Photo/weather work is on accepted main `bddf067`. Notice work is installed and
user-confirmed, but [PR #8](https://github.com/lachlanw-117/Bravia-Ambient-Display/pull/8)
is deliberately unmerged. [The trial record](HOUSEHOLD_ALERTS.md) identifies exact
component revisions and evidence. Merge and deployment remain separate decisions.

The repository presentation/build refresh is a separate local change based on
the notice branch. It deploys no runtime code. ADB remains enabled at the owner's
explicit request; ordinary use does not depend on it.

## Known limitations

- Manual album exports; no unattended Google Photos sync.
- Household-specific compatibility, not certification of all TVs or firmware.
- Full HDMI/app-specific behaviour, controlled outages, real midnight/DST,
  notice overflow and restart continuity are not fully verified.
- Display-only notices; no TV hide/acknowledge controls. HA owns lifecycle,
  routing and phone delivery independently.
- LAN HTTP is unencrypted; photos, options, signing material and caches stay private.

These are product/evidence limits, not automatically assigned tasks. Earlier remote
controls, phone migrations, additional sources, calendar/metadata and ad/launcher
research are not active commitments in this repository.

## Future: separate aircraft screensaver

The sole current future concept is a separate aircraft-tracking screensaver.
It is not required to finish the photo display. Data supply, coverage/accuracy,
permitted use, maps/attribution and application boundaries need a separate plan.
No provider, implementation, release or deployment is selected here.

## Historical plans and compatibility links

The [superseded roadmap](docs/history/roadmap-2026-11-14.md) preserves original
designs, approval boundaries and recovery scenarios as dated evidence. Existing
phase/trial files retain their paths.

### Phase boundaries and stopping points

See the [historical table](docs/history/roadmap-2026-11-14.md#phase-boundaries-and-stopping-points).
It does not reinstate unfinished phases.

### Updates are grouped from the first implementation

The TV keeps one grouped update summary with HA's last-confirmed details and
uncertainty. See the [notice contract](HOUSEHOLD_ALERTS.md#read-only-contract-and-freshness)
and [historical decision](docs/history/roadmap-2026-11-14.md#updates-are-grouped-from-the-first-implementation).
