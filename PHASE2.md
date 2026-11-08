# Phase 2 - Photo presentation

## Status - 8 November 2026

Phase 2 is trial-deployed; final visual acceptance and debugging closeout are
pending. Implementation, TV deployment, commits, pushes and PRs were explicitly
approved. Bravia [PR #3](https://github.com/lachlanw-117/Bravia-Ambient-Display/pull/3)
and companion HA [PR #6](https://github.com/lachlanw-117/Wright-Home-Assistant/pull/6)
remain open; no Phase 2 merge is approved.

Initial deployed source: 95586101a6e70d3ec5c594dc74308701aa89a8f1.
The user liked the design and requested 60-second scenes and a brighter background.
Updated source: 8f0fb3078488e1bcb5585dcf7e0b0f915ae386d6.
Its background tint reduces from about 69% to 50% opacity, retaining the dark
information panel. Installation succeeded; pulling back the installed APK verified
SHA-256: 689528883aee59f52d26269531d90f4caf01892e6d0a8d39b02a26160f91b881.
Setup confirmed 60 seconds persisted through installation, with pairing enabled.
The interval remains configurable from 10 to 300 seconds; the source default is 30.
ADB is currently enabled for final review; Developer options master remains on.
Earlier closed-port results describe prior sessions, not the current state.

## What changes and why

- Design B: fitted photographs occupy the left 72% photo region and a quiet dark
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

Injected Back dismissed the actual dream and returned Preview to setup. The
brighter installed revision was visually inspected in Preview: full portrait pairs,
brighter colour fill and readable dark sidebar rendered correctly. Final user
brightness acceptance, physical remote/ordinary idle checks with debugging disabled
and session closeout remain pending. HDMI, streaming, power, failure/recovery and
older-Android output remain unverified. Private screenshots/profiles stay outside Git.

## Approved trial scope and remaining gate

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
   condition text, missing/stale readings, sidebar readability and subdued fill.
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
