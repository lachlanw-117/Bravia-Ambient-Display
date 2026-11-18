# Public-readiness review

Reviewed 14 November 2026, before changing repository visibility. No publication
blocker was identified within the scope below. This is evidence for a later
owner decision, not permission to publish or a guarantee of vulnerability absence.

## Source and history

The reviewed baseline is `5941e01bac8b214e993b6499831e2449d2d5f268`, including
the notice work from `0c7ce45` and the subsequent documentation tidy-up. Remote
main was `bddf067`; PR #8 remained open and deliberately unmerged. All eight PR
heads were fetched, including the closed unmerged forecast fix. The pre-tidy
inventory covered 49 reachable commits and 240 distinct historical blobs.

Gitleaks 8.30.1, downloaded from its official release and checksum-verified,
found no leaks in the full-history patch scan, all 240 historical file versions,
or the proposed working-tree candidate. Default rules and fully redacted output
were used; reports retain locations/rule names only. No prohibited secret paths
were found in the tracked-path inventory. Credentials, ignored runtime folders,
photos and signing material were not opened. The unrelated untracked local
ad-research note was excluded.

Only three visual assets occur in reachable history: the synthetic SVG overview
and two self-contained HTML comparisons. No raster photos, device screenshots,
binary packages, downloaded SDKs or signing keys are tracked. The SVG and HTML
use fictional content and inline vector artwork, with no remote image/font loads.

Privacy matches were a historic private-LAN health URL, a placeholder LAN URL and
synthetic email fixtures. Commit author email domains were GitHub/noreply domains.
The private address is low-impact operational history, not a public endpoint or
credential. It remains visible in history; archiving is not redaction. No precise
home location, household photograph or employer/client material was identified.

## Licensing and project claims

The existing Apache-2.0 licence is retained. No vendored upstream code or copied
third-party imagery was identified. The authored synthetic assets belong to the
project source; Android/JDK tooling, Node/container software and HA are upstream
platforms with their own terms, not relicensed by this repository. No APK or
container release is being published by this change.

The README distinguishes requirements/design/household acceptance from AI-assisted
implementation and documentation. It avoids Sony, Google or HA endorsement and
limits compatibility claims to recorded household observations. Weather provider
attribution stays in the feed. Ownership/provenance conclusions are based on the
accessible source and project record, not an independent copyright determination.

## GitHub exposure

All eight PR descriptions were inspected; connector readback reported no discussion
comments or attachment links, and issue search returned no issues. The browser
reported no published releases or packages. Both Actions listing pages exposed
35 historical runs, including main pushes and two branch-maintenance runs that
the connector's PR-only run listing omitted.

Latest-attempt logs for all 35 runs (68 jobs) and their artifact listings were
retrieved. Credential/household-data marker checks found no matches; no uploaded
artifacts were listed. Authentication fields in the maintenance output were masked.
This closes the earlier main-push-log coverage gap. Deleted/expired runs, older
rerun attempts and unreachable Git objects are outside the available inventory.
Private local audit helpers/reports remain ignored under `build/public-readiness/`.

## Build and publication boundaries

Local checks passed: 37 host tests, six Markdown-checker tests, all local links,
278 JVM assertions, unsigned Android resources/Java/DEX/assembly/alignment, and
host package syntax/import checks. Explicit SDK/JDK paths with an absent `-Tools`
directory completed unsigned without creating a signing directory. The new
Windows GitHub runner result is recorded on the polish PR's exact-head Checks
tab; a local pass alone does not establish that result.

The setup path, authentication and bounded feed/cache implementation were inspected.
Trusted-LAN HTTP is intentionally unencrypted and unsuitable for direct internet
exposure. Container base images and bootstrap JDK selection are mutable upstream
inputs; this is not a reproducible-byte or container-CVE certification.

Before public visibility, review/land the intended PRs with explicit merge approval,
confirm final-head CI and recheck any changes since this baseline. Existing device
limitations remain documented; this source-only review performs no deployments,
HA changes, ADB changes or device acceptance. Visibility remains private until a
separate explicit owner instruction.
