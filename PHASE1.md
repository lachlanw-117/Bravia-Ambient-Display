# Phase 1 - BOM weather

## Status - 8 November 2026

Authorised local implementation on `feature/bom-weather`, based on reviewed
`main` revision `69ee6877d00eb263ed1c125a273d722639256e31`. Local implementation
and checks are complete. The user approved committing and pushing both Phase 1
branches and opening PRs on 8 November, then approved the feature-branch live trial.
The host and TV have been updated; scoped Phase 1 acceptance and closeout passed
on 8 November, including user-reported ordinary use with debugging disabled.
Merge remains unapproved and the PR stays open. Phase 2 has not started.

The installed BOM adapter source was inspected read-only. An authenticated HA
browser session then verified current weather and retrieved daily forecasts using
`weather.get_forecasts`. Household entity identifiers and the dated readings live
in the [HA companion record](https://github.com/lachlanw-117/Wright-Home-Assistant/blob/docs/bom-weather/docs/bravia_ambient_host.md#phase-1-local-implementation---8-november-2026).
That companion branch contains the household review record.
The initial inspection changed no configuration. The approved selector change and
device trial are recorded below.

## Implementation and reason

- Interpret offset-free daily forecast timestamps as local calendar dates in the
  configured HA timezone. Offset-bearing timestamps still convert into that zone.
  The installed BOM integration returns local midnight without an offset; parsing
  that as the Node host's local time makes date selection depend on its timezone.
  Missing/malformed forecasts remain unavailable; tomorrow never replaces today.
- Store the selected entity with cached weather. On startup, discard weather from
  a different or unidentified entity; same-day forecast fallback also requires a
  matching entity. This avoids carrying Met.no high/low values into a BOM reading.
  Legacy weather cache refreshes on the next successful request; photo caches stay
  unchanged. There is no automatic provider fallback.
- Use HA `last_reported` when present, falling back to `last_updated`. An unchanged
  temperature can still be reported regularly. These timestamps establish HA
  reporting freshness, **not the age of the upstream station observation**. The
  inspected integration marks its weather unavailable after a failed collection,
  but we have not established an independent observation-age check.
- Retain the full supplied attribution in the host feed and source documentation.
  During live review, the user rejected both the full sentence and a smaller
  credit, then explicitly requested no visible TV credit. The final candidate
  removes that line entirely; this supersedes Phase 1's original visible-credit
  design. The photo layout, slideshow, cache and remote dismissal are unchanged.

Reusable packaging retains its existing generic weather default. The household
source is selected through the installed host's existing `weather_entity` option,
as an explicitly approved deployment action. No direct BOM client, additional HA
sensor or credential is introduced. The host continues to use only weather state
reads and the daily forecast retrieval operation described by
[HA weather documentation](https://www.home-assistant.io/integrations/weather/).

## Local validation

- 13 synthetic Node tests passed: existing photo/authentication checks plus BOM
  offset-free/date-only timestamps, four host timezones, Sydney DST transition
  dates, tomorrow rejection, malformed response handling, attribution, HA report
  freshness and isolation between providers. Same-day partial-failure retention
  remains covered.
- Generated the add-on package and checked the packaged server's JavaScript syntax.
- Android Java/resources/DEX build and APK signature verification passed, with all
  15 existing JVM policy assertions. Existing Java 8/deprecation warnings remain.
  The first sandboxed compiler attempt failed on SDK access; the normal local
  build completed with the existing toolchain after that access was allowed.
- Preserved the previous local APK as ignored
  `build/android/wright-ambient-pre-phase1.apk` before building. This is a local
  recovery artifact, not proof that it matches the currently installed TV APK.

Synthetic tests contain no household weather identifiers, coordinates or photos.
The compact-credit revision subsequently passed the Android build/signature
verification and 18 JVM assertions. Host code is unchanged from the 13-test pass.

Initial trial artifact SHA-256 identifiers (superseded TV credit layout):

| Artifact | SHA-256 |
|---|---|
| Candidate APK | `4840b867d8279bec61d85ff0268118797e6227f137d0ffe2709ce8cfd9ce51a5` |
| Preserved previous local APK | `91509f1492ded58e6c87c0b357654e4607993b1fa5ef1e567a44b3b82fd88fd6` |
| Packaged server | `9af7ed17c1da5446749ad2d8ae34b5c757eee494e2815777891a62118a2c2d80` |

## Approved trial - 8 November 2026

The user selected device acceptance before merge, authorised the named trial and
requested that both repositories always follow that order. Workflow and AGENTS
instructions were updated in both repositories. Both PRs remain open.

- Host trial revision: `fe1e045470613f323b31acebb8266fd7fe9ccde5`; server hash
  `9af7ed17c1da5446749ad2d8ae34b5c757eee494e2815777891a62118a2c2d80`.
  The host server source is identical at the later compact-credit revision.
- The previous Pi package matched recorded source `3fbb127`; it was preserved
  before replacing only `server.mjs`. The app was stopped/rebuilt and started with
  BOM selected through the credential-concealed UI. HA core was not restarted or
  reloaded. Source photographs, photo cache and saved access key were preserved.
- Host LAN health returned 200 and unauthenticated `/weather` returned 401.
  Requests to localhost inside the SSH container initially returned 404; those
  were the wrong network namespace and do not describe the host LAN endpoint.
- Initial TV APK installed with `install -r`; pulled installed bytes matched the
  reviewed candidate hash. At about 12:16 Sydney time, a private local screenshot
  showed a fitted photo, 16°C Rainy, today's high 19° / low 11° and the full BOM
  attribution. This establishes authenticated weather reaching the TV without
  reading the saved key. The user found the attribution sentence annoying.
- Revised TV source: `c9f61305bc461796e6f1f5c082bff266c3f5ff4c`; installed compact-credit
  APK SHA-256 `f3f78784e1b4856c14248dd8a2d90cc7084f0ccabfd1352a06916c70e7b60d2a`.
  Installation succeeded and the pulled installed APK matched that hash.
- At this intermediate point, final visual/Back/idle acceptance and closeout were
  pending; their completed results are recorded below. Broader historical HDMI,
  streaming, power, midnight/DST and failure/recovery checks remain unverified;
  no deliberate failure test or TV restart was performed in this trial.

The compact credit was then observed on the actual DreamService, started through
the Sony launcher's visible Screensaver shortcut (`mCurrentDreamIsTest=false`).
It showed 16°C Rainy, high 19° / low 11° with a fitted photo and a small credit.
The user subsequently requested no credit at all. Final TV source
`dc050af8b911351eeee66f522fb15f1958536080` removes visible credit. Its APK installed
successfully after the user re-enabled debugging, and pulled installed bytes
matched SHA-256 `141cf96345e011098247c9c9ab05eee7d85e7e83c53a1a0e2f5cd8e131eeb7d8`.
Build/signature verification and the restored 15 JVM assertions passed. The user
reported the final display "looks right". After debugging was disabled again,
the user answered "its all good" to the explicit Back-dismissal and ordinary idle
photo-return check. Those final physical checks are user-reported, not ADB tests.

Debugging closeout had begun just before that feedback: changing the persistent
ADB port property was refused by the TV, then `adb_enabled` was set to 0. The ADB
connection dropped and TCP 5555 returned ECONNREFUSED. The Developer options master
switch was left enabled to avoid disturbing unreviewed settings. The user was asked to
briefly re-enable debugging for the final no-credit installation, which then
succeeded. After final visual acceptance, `adb_enabled` was set to 0 again and
TCP 5555 again returned ECONNREFUSED. The user then confirmed ordinary idle/photo
playback and Back dismissal with debugging off. Scoped Phase 1 closeout passed.
The persistent port property remains 5555; reboot persistence of debugging-off
has not been tested. Do not re-enable debugging merely to inspect that property.
For a future approved maintenance session, enable USB/ADB debugging in Developer
options, confirm the LAN listener and connection deliberately, and disable it at
closeout. No reboot or Developer-options reset is part of this completed trial.

The actual pre-trial APK was pulled from the TV and matches the preserved previous
APK hash above. Private package/app-backup locations belong in the HA record.
Screenshots contain family photographs and remain ignored local artifacts only.
Do not include them in either PR or publish them.

## Trial acceptance and rollback procedure

1. Review/commit the source and record the exact revision and artifact hashes.
   Recheck the live package for drift and preserve its package and private app
   backup. Confirm the retained previous APK/signing identity before updating.
2. Change only the household host's weather selector through a credential-concealed
   UI; preserve its existing key and other options. Update/rebuild/start only the
   Bravia host package with approval. Do not remove Met.no or restart HA core.
3. With approval, install the reviewed APK over the current app, retaining its
   settings and album. Do not clear data or reimport photographs.
4. Check health/auth rejection, then authenticate through the existing TV setup.
   Compare temperature, condition and today's high/low against HA; attribution
   stays in the feed, with no visible TV credit as requested during review. Confirm
   stale/unavailable behaviour and normal photo playback/dismissal. A changed
   provider's old cache must not be presented as BOM data.
5. Record source/artifact identities, observations and outstanding checks. Follow
   [maintenance closeout](MAINTENANCE.md), including approved debugging cleanup
   and ordinary idle/exit checks with debugging disabled. Do not call the phase
   verified solely because local tests passed.

Rollback, if approved: restore the preserved host package, rebuild/start that app
and restore only its previous weather selector; reinstall the previous matching
signed APK with `install -r`. Preserve options, photos and cache. The source-tagged
new weather cache remains readable by the old host; after selector rollback it
will refresh through the old source. Confirm the feed and overlay again. Restoring
a complete private app backup is a separate recovery choice if package rollback
is insufficient; never expose its contents or clear app data as a shortcut.
