# Design evolution

The finished display grew through household trials: first reliable photos,
then weather that explained its freshness, then useful notices without disturbing
normal TV use. This is a short account of the choices that shaped it. For setup
and current behaviour, use the [overview](../README.md) and [architecture](architecture.md).

## Make the photo display dependable

The initial host/TV split kept a manually exported album on the Pi and gave the
Android screensaver a validated local cache. Android's DreamService owns idle
entry and exit; the app does not compete with viewing using its own idle timer.
Bounded downloads, manifests and decoding make failures containable rather than
letting a partial refresh replace a working album.

The first trials established infrastructure and album transfer. They did not
certify all Sony inputs or firmware. [Initial trial evidence](history/initial-trial-2026-11-07.md)
records that distinction.

## Preserve the photographs

Many album images are portraits. The selected layout pairs consecutive portraits
when suitable and fits whole foreground images instead of cropping them to fill
the TV. A cream sidebar keeps information readable beside the photos. The
household accepted the palette and ordinary idle/Back behaviour in the
[photo-presentation trial](history/photo-presentation-2026-11-08.md).

Later work refined the home-style spacing and image handling. These were
iterations of the same display, rather than a new product direction. The
[11 November visual record](history/tv-acceptance-2026-11-11.md) preserves the
observations and their limits; the [comparison](home-style-comparison.html) uses synthetic content.

## Explain the weather's age and meaning

The household already had BOM weather in HA, so the host normalises that interface
instead of adding a second direct weather client to the TV. Observation temperature
and daily forecasts have different provenance. Today/Tomorrow follow local dates;
missing data must not become a false zero or another day's forecast low.

The weather trials exposed details that compilation could not settle: wording,
attribution placement and household readability. Provider attribution stays in
the feed; the user selected no visible TV credit. Subsequent work also preserved
photo continuation across sessions. See [weather integration](history/bom-weather-2026-11-08.md)
and [weather/continuation refinement](history/weather-refinement-2026-11-08.md).

## Add attention without interruption

HA owns notice meaning, recovery and routing. The TV receives a sanitised,
read-only destination view through the host, with no HA token on the TV. Notices
occupy the bottom-right sidebar only while the screensaver is running. Every
remote input still exits. Earlier interaction and hiding ideas were superseded
by this display-only scope; they are not unfinished commitments.

The trial found a real integration mismatch: HA supplied schema version `"1"`
while the adapter required numeric `1`. The host was corrected to accept exactly
those two representations, retaining strict rejection of other values. The user
then confirmed both natural notice rows appeared correctly. This is scoped visual
acceptance, not proof of every outage or lifecycle scenario. The
[notice trial](history/household-alerts-2026-11-14.md) records exact revisions and
evidence; [current notice guidance](../HOUSEHOLD_ALERTS.md) describes the contract.

## Keep the outcome separate from the record

Photos, weather and household notices meet the owner's current needs. The notice
PR remains deliberately unmerged. [Validation](../VALIDATION.md) identifies what
was tested and what remains unverified without turning those limits into new phases.

The [archive index](history/README.md) holds detailed trials and superseded plans.
They explain the journey without becoming the setup guide. The sole future
concept is a [separate aircraft screensaver](../ROADMAP.md#future-separate-aircraft-screensaver),
which needs its own plan.
