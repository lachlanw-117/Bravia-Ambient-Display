# Repository refresh - 14 November 2026

Branch: `chore/repository-polish`, based on notice head `0c7ce45`.
Audience: technical portfolio reviewers and developers who may reuse the project.

## Changes

- Product-first overview, synthetic illustration, architecture/setup guides and
  documentation index; dated records and their paths are preserved.
- Current product and evidence limits replace the phase-driven active backlog.
  A separate aircraft screensaver is the sole future concept.
- Honest AI-assistance/contribution statement and existing Apache-2.0 attribution.
- Explicit SDK/JDK and unsigned build mode, Windows Android CI, local Markdown
  checks and focused PR guidance. No runtime source/configuration change.

## Verification

Local verification completed on 14 November 2026:

| Check | Result |
| --- | --- |
| Synthetic host suite | 37 tests passed |
| Markdown checker regressions | 6 tests passed |
| Local Markdown links and anchors | 22 documents passed; external URLs not crawled |
| JVM policy/presentation harnesses | 278 assertions passed during the Android build |
| Unsigned Android build | Resources, Java, DEX, APK assembly and alignment passed with JDK 21/API 35/build-tools 35.0.0 |
| Signing isolation | An absent `-Tools` directory stayed absent with explicit SDK/JDK; the existing signed APK hash stayed unchanged |
| Missing prerequisites | Absent JDK and API 35 paths rejected with explicit errors |
| Local app package | Both server modules generated, syntax checked and packaged server imported successfully |
| Offline visual review | README, docs hub, architecture, setup, roadmap and validation rendered at 1280 px and 390 px; both Mermaid diagrams rendered, with no broken images or page overflow |
| Scope and whitespace | No diff in Android application source/resources/manifest, host source or app deployment definition; `git diff --check` passed |

The synthetic SVG uses fictional landscapes and weather/notices. Its sidebar,
contrast and spacing were inspected; the README diagram was changed to vertical
flow for narrow-screen legibility. Render helpers, dependencies and screenshots
remain ignored under `build/repository-preview/`; no project runtime dependency
was added. This local Markdown render approximates GitHub presentation rather
than asserting a hosted GitHub rendering check.

Unsigned artifact: `build/android/wright-ambient-unsigned.apk`, 30,646 bytes,
SHA-256 `a86b1d5ff521c7cefc3cc315e5253d4013f99b14ff3cad704c37e3a187c71154`.
Only existing Java 8/deprecated Android API warnings were observed. The signed
path was retained but not rerun; no signing material was opened for this refresh.

These local results predate the approved push. Hosted CI evidence belongs to
the polish PR's exact-head checks. Saved settings and installed
host/TV configuration were not accessed or changed. Documentation and build
checks do not require device deployment or additional household acceptance.

## Boundaries

PR #8 remains open and unmerged. The original refresh scope was local only;
on 14 November the owner subsequently approved committing the documentation
tidy-up, pushing the polish branch and opening its PR to exercise Windows CI.
The PR targets `feature/household-alerts` to exclude its existing runtime diff.
No merge, release or visibility change is authorised. The separate Google Docs
review guide was updated at the owner's request with the optional-journey step.

No TV/HA actions are performed. ADB remains enabled under the owner's explicit
instruction. The unrelated untracked `HOME_SCREEN_ADS.md` remains untouched.

## Follow-up: a finished entrance and an optional journey

At the owner's request, detailed trials and the original decision log now live
under `docs/history/`, with an archive index and a short `docs/design-evolution.md`.
Current decisions and the notice contract are separated from superseded plans,
intermediate deployment instructions and session deliberation. Old root files
retain compatibility links and anchors; no history was rewritten or discarded.
This follow-up changes documentation only and is included in the approved
polish PR. Local links/anchors passed across 32 documents before adding the
public-readiness record; eight
archived records preserve the original text apart from relocated links, and all
65 previous section anchors remain available. The new journey/index/current guides were rendered at
desktop and narrow widths. No new runtime tests or device actions are needed.

The [public-readiness review](public-readiness.md) records history, assets,
licensing boundaries, Actions exposure and local build evidence. Hosted Windows
CI is checked on the PR before claiming that runner is validated.
