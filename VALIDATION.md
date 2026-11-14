# Validation

Automated checks establish source behaviour and build viability. Dated device
records establish only what was observed. Neither certifies every Sony firmware,
household dependency or failure scenario.

## Run the checks

From the repository root with Node.js 22 or later:

```sh
npm test
npm run check:docs
node tools/package-addon.mjs
node --check build/bravia_ambient/server.mjs
node --check build/bravia_ambient/notices.mjs
node -e "import('./build/bravia_ambient/server.mjs')"
```

On Windows with JDK 21, API 35 and build-tools 35.0.0:

```powershell
./tools/build-android.ps1 -JavaHome $env:JAVA_HOME -SdkRoot $env:ANDROID_HOME -Unsigned
```

The build runs JVM assertions, compiles resources/Java/DEX, assembles an unsigned
APK and checks alignment. Unsigned mode exits before signing-key lookup/creation
and leaves an existing signed candidate alone. See [setup](docs/getting-started.md).

## What is covered

| Check | Evidence | Limit |
| --- | --- | --- |
| Host tests | 37 runtime tests cover photos, weather, authenticated routes and notices | Synthetic fixtures, not a live HA installation |
| JVM assertions | 278 assertions cover cache policy, photo layout/sequence, weather and notices | No Android storage/graphics or physical remote simulation |
| Android build | Resources, Java, DEX and alignment; local signed builds verify signature too | No installation or firmware acceptance |
| Packaging | Both server modules copied and syntax/import checked | Does not build/start the installed HA container |
| Documentation | Local Markdown links/anchors and whitespace checked | External URLs are not crawled |
| Device records | Scoped observations and exact component/artifact identities | Unperformed scenarios remain unverified |

Tests use temporary folders, synthetic images and HA responses, never household
photos or credentials. Production code and assertions are in `test/`, `android/test/`
and `tools/`. New checker regression tests are separate from the 37 runtime tests.

## CI and local tooling

The workflow retains read-only permissions and commit-pinned actions. Host/package
and JVM jobs use Ubuntu. A Windows job invokes the same unsigned build with explicit
JDK/SDK locations and exact API/build-tools versions. There is no signing, release
upload or deployment job. A missing prerequisite fails rather than using another
SDK version. This is repeatable validation, not a byte-identical build claim.
The [Windows runner inventory](https://github.com/actions/runner-images/blob/main/images/windows/Windows2025-Readme.md)
documents hosted prerequisites; the build still checks their exact paths.

Local refresh results and unpublished CI status are in [the refresh record](docs/repository-refresh.md).

## Recorded device acceptance

- Photo presentation and ordinary idle/Back: [Phase 2](PHASE2.md).
- Weather and playback continuation: [weather refinement](WEATHER_REFINEMENT.md).
- Home-style layout: [11 November record](TV_TEST_2026-11-11.md).
- Corrected notice feed and user-confirmed display: [household alerts](HOUSEHOLD_ALERTS.md).

Full HDMI/app-specific coverage, controlled outages, on-device midnight/DST,
real-TV notice overflow and candidate restart/remote continuity remain limited.
These are evidence boundaries rather than new phases. ADB remains enabled by
explicit instruction. This refresh performs no device checks or deployment.

The [7 November snapshot](docs/history/validation-2026-11-07.md) preserves the initial
counts and uninstalled-container notes as history, not current state.
