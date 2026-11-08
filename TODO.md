# TODO

Updated 8 November 2026. The [roadmap](ROADMAP.md) owns the accepted enhancement
scope and separate phase approvals; [decisions](DECISIONS.md) record the why.
Checked items below describe recorded results, not a fresh live inspection.

- [x] Research Google Photos API restrictions and Android DreamService lifecycle.
- [x] Build the Android screensaver, local photo/weather host and offline cache.
- [x] Add automated host and JVM policy tests and verify the signed debug APK.
- [x] Install the Pi host and TV app with approval; record the 147-photo deployment
  from `3fbb127` in [TRIAL.md](TRIAL.md).
- [ ] Test idle entry, first-key exit and non-interruption of streaming and Xbox/HDMI on the Sony.
- [ ] Check rendering, photo orientation, crossfades and offline recovery with an exported album.
- [ ] Determine whether this Sony firmware exposes a configurable idle timeout.
- [x] Verify the initial live daily forecast and TV weather overlay; recorded in TRIAL.md.
- [ ] Check actual TV behaviour at midnight/DST; synthetic coverage is not device evidence.
- [x] Build/start the HA OS local app on the Pi and record health/authentication/weather checks.
- [x] Complete Phase 0 documentation: roadmap, decision log and maintenance procedure.
- [x] Obtain Phase 1 local authorisation, verify the BOM source and prepare tested
  local changes; see [Phase 1](PHASE1.md).
- [x] Commit/push Phase 1, open PRs and perform the separately approved branch trial.
- [x] Finish scoped Phase 1 TV acceptance and debugging closeout; record tool
  evidence and user-reported ordinary playback, idle entry and Back dismissal.
- [x] Obtain Phase 1 merge approval and merge accepted PR #2.
- [x] Prepare the separately authorised Phase 2 photo/sidebar candidate locally.
- [x] Complete Phase 2 approved commits, pushes, PRs and TV trial installation.
- [x] Accept cream palette / 60-second scenes and close Phase 2 debugging.
- [x] Confirm ordinary idle/Back with debugging off; see [Phase 2](PHASE2.md).
- [ ] Obtain separate Phase 2 merge approval and merge the accepted PRs.
- [x] Disable TV debugging and verify TCP 5555 refuses connections; leave Developer
  options master enabled to preserve unreviewed settings. Reboot persistence untested.
- [ ] Consider Google Ambient API partner access only if unattended album sync becomes essential.

Photo presentation and household notices/interactions are accepted future phases,
not implemented features. Aircraft implementation needs its own later plan.
Calendar and photo metadata remain deferred. The MVP was deployed on 7 November;
see [TRIAL.md](TRIAL.md) for results and [VALIDATION.md](VALIDATION.md) for the
initial local-validation snapshot.
