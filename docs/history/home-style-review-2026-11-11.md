> Historical record, archived 14 November 2026. Intermediate plans and pending
> checks are dated evidence, not current instructions or a new backlog. See the
> [current overview](../../README.md) and [design evolution](../design-evolution.md).

# Home-style refinement and Home-screen ad review

Status: layout accepted for local implementation on 11 November 2026; candidate
committed/pushed as PR #5, with exact-head CI passed. The user subsequently approved
the named photo-host update/restart and TV APK trial, now installed. Visual
acceptance is recorded and the debugging port is closed. The user confirmed Back
and normal idle entry with debugging off; tonight's session is closed. Multi-day
low-retention observation and merge approval remain pending.
Ad/launcher work remains research only; no ad-related live change occurred.
The existing installation has several days of user-reported ordinary use without
issues. Do not repeat routine acceptance merely to resume. See
[session evidence](tv-acceptance-2026-11-11.md).

## Layout proposal

[Open the current/proposed comparison](../../docs/home-style-comparison.html).
This is a standalone, offline design board with synthetic illustrations and
weather, not a device screenshot. Its SVG uses the current 1920x1080 logical
layout, 72/28 split, existing text sizes and fixed 66%-height notice divider.
Browser and Android font metrics differ; couch-distance review still matters.

| Element | Existing baseline / screen height | Proposed baseline / screen height |
|---|---|---|
| Date | Uppercase weekday 7%; date 20.5% | Full mixed-case local date 7% |
| Clock and AM/PM | 16% | 16%, same size |
| Now label / temperature | 26.5% / 35% | 22.8% / 30%, same sizes |
| Today label / description / range | 42.5% / 45.5% / 48.2% | 36% / 39% / 41.7% |
| Tomorrow label / description / range | 52.5% / 55.5% / 58.2% | 46% / 49% / 51.7% |
| Freshness | 63.5%, left aligned | 58.5%, right aligned |
| Notice divider / reserved area | 66% / everything below | Unchanged; empty |

Keep cream and charcoal as the dominant palette. Add brighter amber sun details,
blue rain and muted blue clouds; use restrained warm high-temperature and cool
low-temperature accents. Main observed temperature remains charcoal. Colour must
not be the only way to distinguish high/low or conditions. No animation or new
image dependency is needed: extend the existing Canvas glyphs.

Prefer a single date line, but test long weekday/month names. Do not silently
shrink every label: allow a defined two-line date fallback or reconsider spacing
if real font measurements cannot preserve readability. Freshness warnings must
wrap within the area above the divider rather than shrink into unreadable text.
Keep the original full condition text available to accessibility.

The user subsequently requested retaining the last supplied forecast low for its
provider and date, using it when the latest low is blank, with **no extra retained
label and no switch to actuals**. This supersedes the earlier high-only proposal
when a dated low has been captured. A newly supplied low replaces the retained
one. If none was ever captured, the UI still omits the missing Low label/value.
Tomorrow's minimum can become today's only at the matching local date rollover;
it never fills an earlier date. The comparison illustrates the retained-low case.

## Minimum-temperature investigation and candidate status

Research on 11 November found a documented provider pattern, not evidence of a
Java number-parsing failure. BOM's own website release notes describe showing
today's forecast minimum/maximum before 9 am, then only the maximum until 6 pm.
This describes their website, not a guarantee about every HA/API row. Separately,
integration PR #324 reports Day 0 temp_min becoming null after morning; another
HA card's issue #185 includes daily forecast examples with templow null.

The source trace is BOM daily temp_min -> integration daily templow -> host low
-> Android range. Previously our host preserved null and the UI printed Low
followed by a dash. The revised host retains only the same-date/provider minimum
when a current row supplies a high/condition but no low. It also checks unit
compatibility and rejects invalid/future prior check times. A supplied finite
minimum, including zero or a negative value, always takes priority.

The existing atomic weather cache persists the dated low through host restarts.
The additive lowCheckedAt field preserves its original successful check time;
checkedAt remains the time of the current successful row retrieval. Retention by
itself does not mark the row cached or add a warning. Genuine request failures
still retain the whole row with its old check time/cached status as before. Old
dated caches can seed a low using their existing check time. Provider changes and
expired dates cannot reuse a value. No special 9 am job or direct BOM client is
needed: the existing five-minute checks capture lows whenever supplied.

PR #324 proposed filling temp_min from now_temp_later; it was closed unmerged in
favour of #329. That merged replacement addresses condition fallback and explicitly
does not change forecast values. Do not install #324 as a minimum-temperature fix:
an upcoming overnight value must not silently become today's morning minimum.
An observed minimum must not replace a forecast minimum. The user's later approved
same-date retention keeps forecast provenance internally and deliberately adds no
visible retained-value label. An upcoming overnight value from another date is
still excluded.

Sources:
- [BOM website's time-of-day forecast display](https://www.bom.gov.au/news-and-media/website-release-31-march-2026).
- [Integration PR #324 and discussion](https://github.com/bremor/bureau_of_meteorology/pull/324).
- [Merged replacement #329](https://github.com/bremor/bureau_of_meteorology/pull/329).
- [Independent card's null-minimum report](https://github.com/troinine/ha-weather-forecast-card/issues/185).
- [Adapter source](https://github.com/bremor/bureau_of_meteorology/blob/main/custom_components/bureau_of_meteorology/weather.py).

These reports strongly explain the observed pattern but do not prove today's
household payload. Pi SSH returned public-key authentication denied. Browser
inspection was stopped by the user; no further desktop control is needed for
this research. Do not inspect keys/tokens, change authentication, restart HA or
enable TV debugging to work around that limitation. Exact live forecast rows and
installed adapter contents remain a read-only follow-up through approved access.

The layout candidate uses the proposed positions/colours, preserves text sizes,
wraps conditions/status into at most two lines, right-aligns freshness and leaves
the notice region untouched. Full date falls back to two lines when needed rather
than shrinking all text. Full accessible descriptions are retained. Build/signature
verification passed with 248 JVM assertions (15 policy, 208 photo, 25 weather).
The previous local APK was preserved privately before building. All 27 host tests,
including daytime retention, revision, provider/unit/date isolation and a persisted
cache round trip, pass outside the Windows sandbox
(the sandbox denied temporary-file renames). Android rendering, long-date/text
cases and couch readability remain pending;
an APK build does not establish those checks. Host retention source is now changed;
local app packaging and packaged-server syntax checks pass. Android source is
unchanged since its successful signed build. Nothing has been trial-deployed.

## Home-screen advertising only

Research checked 11 November 2026; no current TV firmware/settings inspection.

| Option | What it offers | Limit / recommendation |
|---|---|---|
| Hide individual featured banners | Google documents a long-press Hide action | Per-banner suppression, not a global ad-free switch; availability on this TV unverified. |
| Apps-only mode | Supported simpler Home screen | Google explicitly says sponsored content and teasers remain, and Search/Assistant are unavailable. Do not present as ad removal. |
| Projectivy Launcher | Author advertises an ad-free replacement Home screen | Preferred candidate to evaluate, not yet a selected installed dependency. Verify current release, distribution, permissions and Sony behaviour before installation. |
| Targeted package disablement from skinny-bravia | Potential leads for specific Sony promotions | Guide tested Android 9; household Android 12. No evidence yet that a listed package controls these particular ads. Not selected as the first approach. |
| DNS block lists / factory reset / broad debloating | Wider device/network changes | Not selected: this is a focused Home-screen problem, not a reason to alter unrelated household services or erase setup. |

Recommendation: evaluate a replacement Home screen rather than promise Apps-only
mode removes all promotions. First establish whether voice search and Continue
Watching matter to the household. Keep the stock launcher installed and usable;
do not disable it merely to inspect a candidate. Do not use an automatic install
script from another project.

An approved first launcher trial should install only the reviewed candidate and
open it as an ordinary app, without replacing Home or granting accessibility
permissions. Confirm ad-free appearance, app launching, access to settings/inputs
and performance. This limited trial does not prove Home-button replacement.
A separate named change must review the exact override method, any requested
permissions and rollback before making it the household Home experience.
Do not use a launcher-provided screensaver to replace Wright Ambient.

Before accepting an override, test Home/Back, power/wake, input selection,
streaming return and normal Wright Ambient idle entry/dismissal. A reboot test
needs its own named approval. If idle or inputs regress, undo only the override
and retain the accepted screensaver setup. Actual Sony-specific steps and the
candidate's exact version must be recorded after inspection, not guessed here.

Sources:
- [Google: recommendations, featured banners and Apps-only limitations](https://support.google.com/googletv/answer/10070784?hl=en).
- [Sony: Home-menu ads and recommendations](https://www.sony.com/electronics/support/articles/00225587).
- [Projectivy author's feature/distribution reference](https://github.com/spocky/miproja1).
- [skinny-bravia guide](https://github.com/therealhoodboy/skinny-bravia).

These are source claims, not a guarantee that a feature works on the Sony's
installed firmware. No subscription, purchase, app installation, accessibility
permission, package removal, network change or device action occurred.

## Implementation and approval sequence

1. Review this comparison and resolve household launcher needs.
2. Publish the status/research documentation and runtime candidate as distinct
   commits on the approved focused feature PR. Do not change main directly.
3. Start a focused layout branch from the accepted main baseline. Change only the
   information-panel presentation and the separately requested dated-low retention;
   keep pairing, photo progress, timing and notice lifecycle unchanged. The feed
   change is additive and remains compatible with the prior APK.
4. Build/sign locally, review full/partial/long weather states, long dates and
   freshness warnings at the actual aspect ratio. Use synthetic data and the
   existing relevant harness; avoid unrelated test expansion.
5. Obtain exact Git/trial approvals, then trial the named host update and exact
   branch APK. Retention now requires a host rebuild/restart with its own explicit
   approval; the earlier app-only trial scope no longer suffices. Preserve the
   known-good package and private weather-cache backup so rollback does not lose
   captured minima. Record readability and Back/idle; close debugging.
   Keep the PR open until acceptance and separate merge approval.
6. Treat launcher investigation and any trial as a separate live change. Never
   bundle system-package edits into a photo-layout deployment.

Current stop: local layout/range candidate built and dated-low retention tested;
no actual-temperature source or retained-value label added. Local implementation
was subsequently approved in chat after the comparison review. The feature branch
`feature/home-style-layout` preserves the earlier uncommitted documentation work;
the old docs branch still points to the unchanged main baseline. The user approved
committing/pushing this layout/retention candidate and opening its PR. Publish the
status/research and runtime changes as distinct commits in that focused PR. Keep
it open for the separately approved host/TV trial and successful device acceptance;
merge still requires its own approval. This preparation checkpoint preceded the
approved host/TV deployment recorded below; no launcher change is included.

## Approved trial readiness - 11 November 2026

[PR #5](https://github.com/lachlanw-117/Bravia-Ambient-Display/pull/5) is open against
main at `29b3784f255ee7d29500a03c3166bae05248342d`. Local checks CI run
42997970641 passed. Local APK and packaged-host hashes match the private candidate
inventory. The user approved updating/restarting only the Bravia photo host and
installing the candidate TV APK; no Core restart, launcher change or merge.

Readiness checks found the host/SSH ports reachable and TV debugging unavailable.
The user was asked to enable debugging for this approved trial. Initial SSH
authentication was denied and worked after the user restored existing access, as
recorded below. No installation/rebuild is
claimed from port reachability. Keep current packages and private app/cache backups
before a replacement; do not read saved options, tokens or signing keys.

If the existing cache lacks today's forecast low, the update cannot recover that
value retroactively. Capture tomorrow's supplied low during ordinary five-minute
refreshes and verify date rollover/daytime retention subsequently. Until a valid
matching minimum exists, showing only High is expected. A synthetic retention test
does not by itself verify this multi-day live sequence.

## Approved trial deployment - 11 November 2026

The existing SSH authentication worked after the user restored access; no key,
token or saved option was inspected or changed. The user enabled TV debugging for
the approved session. The actual prior TV APK and Pi package matched the recorded
accepted versions. Current/previous APK copies, preserved package and Bravia-only
app backup are held privately; exact recovery identities are in the ignored trial
inventory, not Git. The private backup file was verified present before replacement.

Only server.mjs changed in the Pi package. The uploaded source hash was verified,
then local_bravia_ambient was stopped, rebuilt and started successfully. Live
package hash matched source from PR head 29b3784f255ee7d29500a03c3166bae05248342d.
Health returned 200/ok and unauthenticated weather returned 401. App runtime stats
were available. This establishes service operation, not an authenticated forecast
or a multi-day retention pass. No Core restart, Thread/SSH configuration change,
provider switch or credential change occurred.

The candidate APK installed with install -r, preserving app data. The actual
installed APK was pulled back and its hash matched the recorded signed candidate.
An external launch of the non-exported Preview activity was rejected by Android;
no manifest/security setting was changed. The user was asked to open Preview via
the normal app and report date/clock, colour, spacing, freshness and physical Back.
The user accepted the appearance and supplied a private photo showing the full
date above the clock, paired complete photos, the tighter cream weather column,
colour accents and right-aligned Checked 1 min ago. Today showed High 27 with no
low; Tomorrow showed High 24 / Low 19. This is authenticated app-rendered weather
evidence, not a direct inspection of upstream rows or stored-cache contents. The
photo is not copied into Git. The missing Today low is consistent with no earlier
captured value; that explanation is not independently proved for this date.

ADB was disabled after visual acceptance. The command disconnected, and a fresh
TCP probe confirmed the TV actively refused port 5555. Developer options master
was not changed. The user was asked to confirm physical Back and ordinary idle
entry with debugging off; the user replied that all works. Scoped visual/ordinary
use acceptance and session closeout are complete. No debugging re-enable is needed
to inspect Tomorrow becoming Today during ordinary use.

Do not mark the dated-low lifecycle verified solely from installation. A same-date
low must first be captured; overnight promotion and later blank-response retention
need their own subsequent observation. Preserve these limits when closing the
visual trial. The PR remains open and main is unchanged.
