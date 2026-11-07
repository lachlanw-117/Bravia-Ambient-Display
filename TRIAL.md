# Infrastructure trial without photos

Test one dependency at a time. No Google account or family photos are needed.
The TV APK was installed on 7 November 2026 and the user confirmed that the
screensaver works as expected. The Pi host was installed and started later that
evening; authenticated weather and the TV overlay were verified without photos.

## Recorded results

- TV APK installation: ADB returned Success; the user confirmed the app is visible.
- Screensaver behaviour: user reported working as expected. Individual streaming,
  HDMI, remote-button and power checks have not been separately recorded.
- TV OS version: Android 12. System-picker availability: not yet recorded.
- Local host validation: all 10 synthetic Node tests pass; local app package generated.
- Pi host: version 0.1.0 from `b05ca1f6bbf3f8151c950150ac5cdd7bb5dbd826`,
  installed as `local_bravia_ambient` and started on HA OS 18.4 / aarch64,
  Core 2026.11.0. The existing Terminal & SSH app exposes local packages at
  `/local_apps`. No previous Bravia package or photo folder existed; no existing
  installation files were replaced. All three generated package hashes matched.
- LAN health: HTTP 200 with `{"ok":true}` at `http://192.168.0.142:8099/health`.
  Unauthenticated weather returned HTTP 401; authenticated manifest had zero photos.
  The source folder `/share/bravia-ambient` was created and remains empty.
- Authenticated live weather around 22:08 Australia/Sydney on 7 November 2026:
  15.1°C, `partlycloudy`, today's high 15.1°C / low 14.4°C; both stale flags false.
- TV connection around 22:10: host URL and random access key saved; Preview showed
  **Weather: Feed received**, **15°C partlycloudy**, **High 15° Low 14°**.
  The TV rounds the host readings. **No album cached** is expected with zero photos.
  The key was not printed or committed; no existing credential files were opened.
- Failure/recovery: pending. No stop/restart test or HA core restart was performed.
- Follow-up: human-readable weather labels prepared locally after the trial;
  the installed APK still displays raw `partlycloudy`. Updated APK installation
  requires separate approval. Local Android build, DEX, signing verification and
  all 15 existing JVM assertions passed. The previous APK was preserved locally.
  Visual verification of the updated labels remains pending installation.
  No cache implementation change is included.
- Persistent offline photo storage is no longer required by the user. The installed
  APK still caches photos; no storage implementation change has been made.

## Trial sequence

1. **TV application only.** Sideload the APK and open Preview without configuring
   a host. Expect a diagnostic screen, moving local clock/date and unavailable
   weather. Check Back/Home and reopening Preview. This proves installation and
   rendering; it does not prove automatic screensaver entry.
2. **TV idle lifecycle.** Select the screensaver through the system picker if
   available. Test launcher idle, remote dismissal, streaming, paused playback and
   Xbox/HDMI. Stop here if the firmware cannot support the required behaviour.
   Restore the original screensaver if it interrupts ordinary use.
3. **Pi host.** Install/start the packaged HA local app with a random access key.
   Leave the photo folder empty. Check `http://PI_ADDRESS:8099/health` from a LAN
   browser: expect `{"ok":true}`. This proves the port is reachable, not authentication
   or HA weather. A missing source directory may log a photo-sync warning.
4. **Authenticated TV connection.** Enter the host URL and key in the TV setup,
   save and reopen Preview. Expect Weather: Feed received. A wrong key or blocked
   port gives Feed unavailable. Photo status can remain "No album cached" for an
   empty feed; that message also covers connection failure and is not an auth test.
5. **HA weather.** Look for real temperature, condition and today's high/low in
   the overlay. Feed received alone does not prove the HA request succeeded.
   Missing readings isolate the remaining work to the host-to-HA path.
6. **Failure recovery.** With approval, stop/restart the host and reopen Preview.
   Clock/date must keep working. Cached weather must be labelled stale when aged.
   Confirm recovery; refresh occurs on entry and every five minutes while running.

Only then add a small exported album to test decoding, crossfades and offline photo
playback. A photo-free trial cannot validate those components. Record the TV's OS
version, picker availability, each result and any error before changing architecture.
