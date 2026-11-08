# Phase 2 - Photo presentation

## Status - 8 November 2026

Phase 2 is trial-deployed; the user accepted the cream palette. Debugging is
closed; the user confirmed ordinary idle entry and physical Back dismissal both
work normally with debugging disabled. Relevant Phase 2 acceptance is complete;
separate merge approval remains required. Implementation, TV deployment, commits, pushes and PRs were explicitly
approved. Bravia [PR #3](https://github.com/lachlanw-117/Bravia-Ambient-Display/pull/3)
and companion HA [PR #6](https://github.com/lachlanw-117/Wright-Home-Assistant/pull/6)
were merged after acceptance and explicit approval. Bravia merge:
6076abeda6acfb7e8fb6f11418917c1faf05d944; companion merge:
729e9f98c528c10a88f836c75d42442ed1d0045d. This completed Phase 2; the subsequent
weather refinement has its own open PRs and trial record.

Initial deployed source: 95586101a6e70d3ec5c594dc74308701aa89a8f1.
The user liked the design and requested 60-second scenes and a brighter background.
First brighter trial source: 8f0fb3078488e1bcb5585dcf7e0b0f915ae386d6.
Its background tint reduced from about 69% to 50% opacity, retaining the dark
information panel. Installation succeeded; pulling back the installed APK verified
SHA-256: 689528883aee59f52d26269531d90f4caf01892e6d0a8d39b02a26160f91b881.
The user found that trial still too dark and asked for cream/white with dark text.
Current installed source: a8aa2f6f29ac57901058a17e2a6feb721d8c973a.
Current installed APK SHA-256, verified by pulling it back:
3345307a2d078b41b2e3094970a151771843470ea04d66f6849b34cc77b26553.
A warm cream veil now lightens the photo-derived wash; the sidebar is opaque cream
with charcoal text, darker weather icons and pale photo borders. Foreground photo
pixels, layout, timing and rendering workload are unchanged. The prior darker APK
is privately preserved as build/phase2-trial-2026-11-08/installed-brighter.apk.
Local signed build and all 215 Android assertions passed for the cream revision.
Cream-source CI passed in Local checks run 42448152522.
Native Preview showed cream backgrounds, dark readable text and full portrait
pairs; setup retained 60 seconds and pairing. The user confirmed the cream design
looks right. Ordering remains unchanged following their clarification. This replaces the original dark-palette
direction following actual viewing feedback, not a new phase or theme-switch feature.
Setup confirmed 60 seconds persisted through installation, with pairing enabled.
The interval remains configurable from 10 to 300 seconds; the source default is 30.
After cream acceptance, ADB was disabled and TCP 5555 returned ECONNREFUSED.
Developer options master remains on to preserve unreviewed settings. Reboot
persistence is untested. The user subsequently confirmed both ordinary idle entry and physical Back
dismissal work normally with debugging disabled.

## What changes and why

- Design B: fitted photographs occupy the left 72% photo region and a quiet cream
  information panel sits at the right. A large light-weight clock, date, weather
  glyph, current reading and today's high/low replace the small photo overlay.
  No visible provider credit is reintroduced. No fake notices or aircraft UI is
  shown in the space reserved for later phases.
- Pair two consecutive portrait images when the photo region is wide enough.
  Landscape and square images stand alone. An unmatched final portrait remains
  alone, instead of wrapping into the next album cycle. Group order follows the
  manifest; an album change restarts traversal at its beginning. Corrupt/missing
  photos are skipped. Setup includes a single-photo option; it changes grouping,
  not cache contents or album order. The configured interval advances a scene.
- Every foreground image uses complete-image fitting. A tiny static colour wash
  derived from the current group fills residual space instead of black bars.
  Background scaling is deliberately abstract; foreground photographs are not
  stretched or cropped. Decorative frames sit outside complete image bounds.
- Crossfade current/incoming photo scenes for 1.5 seconds, with the information
  panel stable. No pan/zoom, continuous blur, external map, web renderer, shader
  dependency or animated background is added.
- Read source orientation before selecting pairs, use ImageDecoder on API 28+
  and native EXIF transforms on the API 26-27 fallback. The Sony runs Android 12;
  older-platform EXIF output still needs actual device validation.
- Decode against actual photo slots within a maximum 1920 x 1080 logical surface,
  including on a 4K TV. Decode into sRGB to bound ordinary bitmap memory instead
  of retaining wide-gamut buffers. Never upscale during decoding. Retain current/incoming
  groups only; release the outgoing group after its fade. A scene background is
  only 48 x 27 pixels. JVM geometry cases bound the two groups' foreground bitmap
  pixels below 16 MiB; this is not a measured total Android/GPU memory guarantee.
- Weather refresh gets an independent worker/lock so slow photo transfer/hash
  checking cannot hold it up. Photo pruning is restricted to photo/manifest
  transactions so it cannot delete the concurrent weather transaction. Cancelling
  a scene interrupts workers and disconnects active HTTP requests. Existing photo
  cache sizes, format and transactional semantics are retained.
- Build intermediates use a fresh ignored directory per run so removed Java
  classes cannot leak into a later APK. Signing identity and stable APK output
  path stay unchanged; no previous artifact is deleted.

Android's native [ImageDecoder](https://developer.android.com/reference/android/graphics/ImageDecoder)
and [EXIF interface](https://developer.android.com/reference/android/media/ExifInterface)
provide decode sizing/orientation; no image library is added. Household mappings
and deployment notes remain in the companion HA repository.

## Local evidence

- Android Java/resources/DEX build and APK signature verification passed.
- Existing 15 policy assertions passed.
- 200 new synthetic JVM assertions passed: complete-image fitting and aspect
  preservation, portrait grouping, unmatched-last ordering, single-photo mode,
  landscape/square rejection, small inputs, 720p/1080p/4K decode budgets, a narrow
  surface fallback and weather/photo transaction isolation. These exercise the
  actual layout/sequence/pruning functions used by the app.
- CI and the local build run the same JVM harness. Initial checks passed, as did
  updated source 8f0fb30 in Local checks run 42447727049. The updated local APK
  again passed all 215 Android assertions, build and signature verification.
- Initial trial APK SHA-256:
  `d9ca612c35218fa2c5578399cfb160d44a1b3d4dad8d741fd6606726bbb6b922`.
- The accepted Phase 1 APK is preserved privately at ignored
  `build/phase2-trial-2026-11-08/phase1-known-good.apk`, SHA-256
  `141cf96345e011098247c9c9ab05eee7d85e7e83c53a1a0e2f5cd8e131eeb7d8`.
- A read-only maintenance connection confirmed that same hash on the actual TV,
  preserved it as `build/phase2-trial-2026-11-08/current-phase1-installed.apk`,
  and confirmed `AmbientDream` was running with `mCurrentDreamIsTest=false`.
  Its one-sample baseline was TOTAL PSS 21,666 KiB, TOTAL RSS 100,924 KiB and
  native heap PSS 13,948 KiB. These describe Phase 1, not candidate performance;
  compare like-for-like active scenes before drawing a conclusion.

## Device evidence and remaining limits

The initial candidate ran as the actual Sony dream, with mCurrentDreamIsTest=false.
Private screenshots showed successive complete portrait pairs, a single portrait,
clock and BOM current/high/low, without provider credit. Single-photo mode was
saved and visibly checked, then pairing restored. Landscape/square-alone behaviour
is covered by synthetic tests; a live landscape scene is not yet explicitly
recorded. Selected orientation looked correct; not every album photo was reviewed.

Startup: PSS 40,530 KiB / RSS 122,588 KiB, 5 janky frames of 88. Later warm sample:
PSS 37,922 KiB / RSS 120,656 KiB. After resetting frame statistics, a short
transition sample recorded 177 frames, zero janky frames, median/p95 5 ms and
p99 10 ms. These initial-candidate observations do not establish long-run memory
stability or performance for every photo/device. Do not infer a memory improvement
from the Phase 1 baseline with different content.

Injected Back dismissed the actual dream and returned Preview to setup. The earlier
brighter dark revision was inspected in Preview before user feedback prompted the
current cream revision. The user accepted the cream palette and debugging was closed. The user confirmed physical Back and ordinary idle entry with debugging disabled. HDMI, streaming, power, failure/recovery and
older-Android output remain unverified. Private screenshots/profiles stay outside Git.

## Album ordering

The host sorts source filenames with localeCompare and the app follows that
manifest order, not EXIF date or shuffle. Consecutive portraits may share a scene;
other photos display alone. Each scene lasts the saved 60 seconds. The sequence
loops at the end and starts at the beginning of every new dream/Preview session.
An actual album-list change also resets traversal. Routine identical refreshes do
not reset it. Repeated development installs/previews explain seeing early photos
again. Private screenshots of the uninterrupted cream preview confirmed different
pairs at successive minutes. The user asked for an explanation, not an ordering
change; no shuffle or persistent cursor was added.

## Completed trial scope and remaining merge gate

The source commit/PR gate passed. Installations used only TV APK `install -r`,
preserving its key, settings, cached album and signing identity. The user approved
that Phase 2 trial; this is not permission to merge or deploy later phases.
No host rebuild, BOM selector change, HA config change, core restart/reload,
cache removal, photo reimport or system-package cleanup is needed.

Record the exact trial commit and installed APK hash. Inspect the current TV APK
and preserve it before replacement, then compare the following:

1. Portrait pairs, landscape alone, odd portrait remainder, whole-image fitting,
   orientation and order across multiple transitions. Check the single-photo
   setting without exposing credentials in setup. Capture only private evidence.
2. Clock/date and current BOM weather/today's high/low, no credit line, long
   condition text, missing/stale readings, sidebar readability and warm cream fill.
3. Native memory/frame behaviour across several transitions and prompt exit.
   A locally bounded bitmap calculation does not prove smooth firmware rendering.
4. Remote Back/first input and normal idle re-entry. Home/power remain system-owned
   and no new interaction handler is added. Do not claim historical HDMI/streaming
   scenarios passed just because this candidate dismisses normally.
5. User visual acceptance when they return, then debugging closeout and ordinary
   use with debugging disabled. Keep the PR open until relevant acceptance and
   closeout are recorded. Do not turn off debugging before final visual feedback
   while a user is actively reviewing the trial; record its state at handoff.

Rollback, if needed with the appropriate approval: reinstall the preserved,
matching-signed Phase 1 APK with `install -r`. Candidate and baseline retain version
code 1, so no downgrade bypass, uninstall or data clear is needed for this trial.
Host and household configuration are unchanged. Verify photo/weather playback
and ordinary dismissal again and record the actual outcome. Artifact hashes,
not version 0.1.0 alone, identify these trial builds; a release/versioning policy
for a published milestone remains separate.

## Accepted weather refinement design - not implemented

Household feedback accepted the cream palette and existing native text sizes,
but requested explicit Now/Today labels, today's forecast condition, tomorrow's
condition/high/low, and honest freshness information. A first mockup was too dense
and oversized. The revised concept moves information upward, preserves small
native-style text and reserves the entire area below the existing divider for
future HA notices. No placeholder notice appears when the area is empty.

The user accepted the revised minimalist mockup. This accepts the design, not
permission to deploy a weather-feed/schema change.
Verify BOM's daily forecast fields and actual timestamp availability before building.
A last-successful-check label must not be presented as observation or forecast issue
age. The revised preview uses illustrative scenic photographs and weather values;
no household photo was sent for image generation. Tomorrow's forecast and these
new labels are not in the installed APK. Existing Phase 2 acceptance remains scoped
to the installed cream photo-presentation revision.

## Unplanned outage observation - 8 November 2026

At approximately 16:24 Australia/Sydney, following the user's report of an
electrical interruption affecting internet, LAN and Pi, read-only checks found:

- HA HTTP returned 200 and its normal web port accepted connections.
- Photo host health returned 200 with ok=true; unauthenticated weather returned
  401. This proves service reachability and rejection, not authenticated weather
  freshness or the complete album.
- TV ADB port returned ECONNREFUSED: the closed debugging port stayed closed
  through the reported incident. The check does not independently prove TV reboot.
- The previously used Pi SSH connection returned connection refused. No remote
  command ran, so app startup policy, uptime and Core/app process status could not
  be established over that route. The user subsequently reported that Terminal & SSH and the Thread border router
  had not recovered, and manually started both in HA UI. Their startup settings
  and the cause of the failure were not independently inspected. The agent made
  no restart or configuration change.
- The user confirmed photos and weather look normal after the outage, with no
  reported waiting/cached warning. This establishes visible recovery, not verified
  BOM observation age. Internet/BOM upstream recovery and full HA device health
  have not been established by the HTTP checks.

No live repair, deployment, service restart or debugging enablement was performed.
This is partial recovery evidence, not a blanket failure/recovery acceptance pass.

The accepted weather concept is now scoped in [WEATHER_REFINEMENT.md](WEATHER_REFINEMENT.md);
that document records source semantics, compatibility, validation and the separate
trial gate. The user subsequently authorised local refinement implementation on
separate stacked branches; no refinement deployment or merge has occurred.
