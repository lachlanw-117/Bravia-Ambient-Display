# Phase 1 - BOM weather

## Status - 8 November 2026

Authorised local implementation on `feature/bom-weather`, based on reviewed
`main` revision `69ee6877d00eb263ed1c125a273d722639256e31`. Local implementation
and checks are complete. The user approved committing and pushing both Phase 1
branches and opening PRs on 8 November. Merge and live deployment remain pending
separate approval. Phase 1 is not deployed or TV-verified. Phase 2 has not started.

The installed BOM adapter source was inspected read-only. An authenticated HA
browser session then verified current weather and retrieved daily forecasts using
`weather.get_forecasts`. Household entity identifiers and the dated readings live
in the [HA companion record](https://github.com/lachlanw-117/Wright-Home-Assistant/blob/docs/bom-weather/docs/bravia_ambient_host.md#phase-1-local-implementation---8-november-2026).
That companion branch contains the household review record.
No integration, saved host options or device configuration was changed.

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
- Display the provider's supplied attribution beneath weather in the existing TV
  overlay. The photo layout, slideshow, cache and remote dismissal are unchanged.

Reusable packaging retains its existing generic weather default. The household
source is selected through the installed host's existing `weather_entity` option,
as a separately approved deployment action. No direct BOM client, additional HA
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
No TV visual, remote, idle, power or network-failure test was performed in this
phase. No ADB/device action, host restart or HA reload/restart was performed.

Local artifact SHA-256 identifiers (uncommitted build, not a deployed revision):

| Artifact | SHA-256 |
|---|---|
| Candidate APK | `4840b867d8279bec61d85ff0268118797e6227f137d0ffe2709ce8cfd9ce51a5` |
| Preserved previous local APK | `91509f1492ded58e6c87c0b357654e4607993b1fa5ef1e567a44b3b82fd88fd6` |
| Packaged server | `9af7ed17c1da5446749ad2d8ae34b5c757eee494e2815777891a62118a2c2d80` |

## Proposed deployment and acceptance - approval required

1. Review/commit the source and record the exact revision and artifact hashes.
   Recheck the live package for drift and preserve its package and private app
   backup. Confirm the retained previous APK/signing identity before updating.
2. Change only the household host's weather selector through a credential-concealed
   UI; preserve its existing key and other options. Update/rebuild/start only the
   Bravia host package with approval. Do not remove Met.no or restart HA core.
3. With approval, install the reviewed APK over the current app, retaining its
   settings and album. Do not clear data or reimport photographs.
4. Check health/auth rejection, then authenticate through the existing TV setup.
   Compare temperature, condition, today's high/low and full attribution against
   HA. Verify attribution fits and remains readable over photographs. Confirm
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
