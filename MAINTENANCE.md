# Maintenance, acceptance and cleanup

This procedure was recorded during Phase 0 on 8 November 2026. It is not a record
of completed device actions. Follow [AGENTS.md](AGENTS.md), the companion HA
repository's instructions and the [roadmap](ROADMAP.md). Approval for one named
action does not cover deployment, restarts, cleanup, commits/pushes or merges
outside its scope.

## Before a maintenance session

Runtime trials deploy an exact feature-branch commit while its PR stays open.
Successful relevant device acceptance and recorded closeout precede merge
approval; `main` is the accepted baseline. See [CONTRIBUTING.md](CONTRIBUTING.md).

1. Name the phase and exact local work. Inspect branches, working trees and
   relevant live drift without opening credentials or saved app options.
2. Record the reviewed revision/local diff and relevant APK/package hashes.
   Package version 0.1.0 alone does not distinguish the recorded deployments.
3. Run appropriate synthetic/local checks and review the diff. Keep visual and
   device checks explicitly pending until observed.
4. Describe the exact live changes and tests for approval. Include any debugging
   enablement, reload, stop/start, restart or failure simulation required.
5. Preserve the previous known-good APK/package and an appropriate private
   backup before approved replacement. Never print or commit backup contents.

Do not reinstall the app merely to resume work. Do not change its signing key or
clear data as a shortcut; settings and caches can be lost.

## Record acceptance honestly

For each approved change, record date/timezone, action and scope, source revision,
checks and observations, evidence source (tool output or user report), limitations,
rollback and remaining work. Record exact deployed hashes when obtained without
exposing private data. Distinguish implementation from deployment and both from
verification.

Relevant checks include orientation/fitting, portrait groups, ordering,
crossfades, couch-distance readability, memory/frame behaviour, idle entry,
streaming/paused-video/HDMI, remote buttons and power. Verify feed freshness,
forecast day and recovery separately. An APK build or a successful installation
does not establish these behaviours. Failure tests and restarts require their
own approved scope.

## Close an approved TV testing session

1. Finish recording results and preserve the current/previous recovery artifacts.
2. Disable ADB/network debugging using the approved method. Do not confuse
   disconnecting the development computer with disabling the TV listener.
3. Review the Developer options master switch and any deliberately changed
   settings before turning that switch off. Do not reset unrelated options.
4. With debugging disabled, confirm photo playback, normal idle entry and prompt
   remote dismissal through ordinary TV use. Use user observations where ADB is
   no longer available; do not silently re-enable it to complete the check.
5. Record the final debugging/settings state and how the next explicitly
   approved maintenance session will re-enable only what it needs.

The installed app does not need ADB for normal playback. Android documents
[ADB](https://developer.android.com/tools/adb) and
[Developer options](https://developer.android.com/studio/debug/dev-options)
separately; Sony menus and behaviour still need observation. The last recorded
trial had debugging enabled. The later [Phase 1 record](PHASE1.md) documents
debugging disabled, TCP 5555 refusing connections and user-confirmed ordinary use
on 8 November. The master switch remains on to preserve unreviewed settings;
reboot persistence was not tested. Consult the latest trial record, not this
historical baseline, before a future approved maintenance session.

If the session ends before cleanup or a check can be completed, record the
remaining item and its reason. Do not report the session fully verified/closed.

## Keep, inventory and delete are separate actions

Keep current and previous known-good APK/package files, signing material, needed
backups and rollback instructions outside version control. Household notes record
the known rollback locations; verify their existence before relying on them.

Temporary build, transfer and conversion artifacts may be inventoried without
deleting them. Propose exact paths and prove they are disposable before any
separately scoped deletion. Never read/disclose credentials while checking them.

Cleanup does not include clearing app data or photo caches, deleting source
photos/backups/signing material, uninstalling system packages, applying
skinny-bravia, disabling HA Terminal & SSH, or moving toolchains shared with the
HA repository. Storage redesign and toolchain consolidation remain deferred.

## Roll back only the affected change

Review configuration/data compatibility, preserve newer unrelated changes and
restore only approved files/settings from the known-good revision or backup.
Obtain the required approval for installation, app rebuild/start, reload or
restart. Verify the restored behaviour and record what actually happened.

Selecting the original system screensaver, stopping the host, uninstalling the
app and clearing its data are different operations, with different effects.
Do not combine them into an automatic rollback or cleanup script.
