# Development workflow

`main` is the reviewed source baseline. It is not a claim that every change is
installed on the TV or Pi. [TRIAL.md](TRIAL.md) and the companion HA deployment
notes identify actual deployed revisions and remaining acceptance checks.
Read [AGENTS.md](AGENTS.md) before acting.

## One coherent change at a time

1. Start a short-lived branch from current `main`, for example
   `feature/bom-weather`, `feature/portrait-layout` or `docs/trial-results`.
2. Keep changes focused, preserve unrelated work and use synthetic test data.
3. Run the relevant local checks and open a pull request describing the problem,
   resulting behaviour, validation and limitations. Link the roadmap phase and
   companion PR when both repositories change.
4. Review the exact diff and CI results. Obtain the required merge approval,
   merge the finished work and retire the incorporated branch.
5. Treat deployment as a separate approved action. Record its exact revision,
   verification and rollback, including any unverified physical behaviour.

A phase can use several PRs. No permanent development or staging branch is
needed. Do not rewrite published history to make it look tidy. Old work that is
superseded rather than merged needs an explicit audit and a recoverable reference
before its branch is retired.

Bravia owns reusable Android/host code, packaging, interfaces and tests. HA owns
household configuration and deployment notes. Keep personal location, photos,
options, runtime data, credentials, backups and signing material out of Git.
The repository's visibility is a separate decision; branch consolidation does
not authorise making a private project public.

## Automated and local checks

GitHub Actions runs the Node host tests, local app packaging/server syntax check
and JVM policy assertions on pull requests and pushes to `main`. Its token has
read-only repository permissions, and action dependencies are pinned to commits.
It does not sign/publish an APK, connect to HA/TV or deploy anything.

Host checks need Node 22 or later:

```sh
npm test
node tools/package-addon.mjs
node --check build/bravia_ambient/server.mjs
```

Run the policy harness with JDK 21, creating the output directory first:

```sh
mkdir -p build/policy-tests
javac -d build/policy-tests android/src/au/wright/ambient/Policy.java android/test/PolicyTest.java
java -cp build/policy-tests PolicyTest
```

The full Windows APK build remains the separate process in [README.md](README.md).
CI policy assertions do not replace Android compilation, signature verification
or Sony rendering/input/performance tests.

## Milestones and releases

Use an appropriate version increment and a release tag for a reviewed software
milestone rather than keeping distinct deployments under the same version string.
Release notes should explain changes, validation, known limitations and upgrade
implications. A tag identifies source; it does not prove a household deployment.
No release, APK publication or signing-key change is part of repository
consolidation. Follow [MAINTENANCE.md](MAINTENANCE.md) for approved device sessions.
