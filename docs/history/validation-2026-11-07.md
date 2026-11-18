> Historical record. Current guidance: [documentation index](../README.md). Earlier plans and pending notes are retained as dated evidence, not active commitments.

# Local validation

This is the historical initial local-validation snapshot from 7 November 2026.
No installation, live application weather request, device action or deployment
was performed during those checks. Later deployment and the 11-test album-limit
validation are recorded in [TRIAL.md](../../TRIAL.md); statements below about an
unbuilt/uninstalled container apply to this earlier snapshot only.

Phase 0 on 8 November adds documentation and reconciles status descriptions only.
Its Markdown-link/whitespace checks do not revalidate Android, the host or live
devices. Accepted enhancements are tracked in the [roadmap](../../ROADMAP.md).

| Component | Result |
|---|---|
| HA reliability harness | 3 unittest cases; 13 charging scenarios, 12 washer transitions, unrelated definitions preserved |
| Node host | 10 tests: transactional import, deduplication, restart/offline retention, replacement, size limits, authenticated read-only routes, local-day forecasts and partial weather failure |
| Android policy | 15 JVM assertions: slide timing, decode bounds, content IDs, duplicate/oversized feed rejection and total cache budget |
| Android app | API 35 resources, Java and DEX compile; debug APK signing and signature verification pass |
| HA local app packaging | Build directory generated with config, Dockerfile and server; container image not built or installed |
| Repository | Changed YAML parses, local Markdown links resolve and whitespace check passes |

The automation harness renders the actual proposed templates; it is not the HA
runtime. JVM assertions cover platform-independent policy, not Android rendering
or the on-device AtomicFile implementation. The host tests use temporary folders,
a tiny synthetic PNG and mock HA responses. No family photographs were inspected.

The Android build reports Java 8 compatibility warnings and a deprecated fullscreen
window API in the preview activity; it succeeds. There is no Android emulator in
this environment. Visual quality, lifecycle behaviour on Sony firmware, power and
HDMI interaction, first-key handling, actual daily forecast output, real-photo
decoding and HA app container integration remain acceptance checks.

Network refresh and image decoding use separate worker executors so a slow host
does not hold up slideshow progression. Downloads have byte and timeout limits;
the last complete manifest stays active until the replacement album is validated.
Clock/date continue locally if the server is offline; cached weather is labelled
and yesterday's high/low is withheld after midnight.

Source, build and deployment instructions are in [README](../../README.md).
The generated APK is under `build/android/wright-ambient-debug.apk`, ignored by Git.
