# Phase 2 - Photo presentation

## Status - 8 November 2026

Local implementation is prepared on `feature/photo-presentation`, based on
accepted Phase 1 merge `49f8b8e8173e8265a58a08d24032f665657d43d3`. The user approved
the Phase 2 TV trial and briefly re-enabled debugging before stepping away.
The user subsequently explicitly approved Phase 2 commits, pushes and PRs on
8 November. The candidate is prepared for that Git/PR gate; it is not deployed,
device-tested or merged. The installed baseline remains the accepted Phase 1
no-credit APK. While Git approval was pending, debugging was closed again and
TCP 5555 returned ECONNREFUSED.
Developer options master remains enabled. A future trial connection will need the
user to briefly re-enable USB/ADB debugging again.

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
- CI and the local build now run the same new JVM harness. CI results remain
  pending an approved push/PR; the full APK build is local, not a CI deployment.
- Current local APK SHA-256:
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

These are local/synthetic results. Rendering, native decode orientation, actual
heap/GPU usage, frame pacing, concurrent network cancellation and Sony dismissal
have not been verified for this implementation. Photos/screenshot evidence stay
outside Git, and no precise household location is used in fixtures or source.

## Approved trial scope and remaining gate

After the required source commit/PR gate, update only the TV APK with `install -r`,
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
