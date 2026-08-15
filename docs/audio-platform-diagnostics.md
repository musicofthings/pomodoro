# Audio Platform Diagnostics

_Verified on August 15, 2026 with Xcode 26.3, iOS Simulator 26.2, and the iPhone 17 simulator._

## Why this document exists

Cozy Focus ambient tracks are 30-second local M4A files. During simulator testing, audio appeared extremely quiet and sometimes appeared to stop at the first 30-second boundary. Clearing app data, DerivedData, and the simulator did not resolve the behavior because the main failure was an audio-route lifecycle issue rather than stale state.

The Android emulator implementation must be tested against the same failure modes later: emulator/device volume, host output routing, focus loss, output-device changes, and recovery of a looping player after its audio sink is reconfigured.

## Verified iOS findings

### Simulator volume is separate from the app gain

- `AVAudioSession.outputVolume` initially reported a low simulated-device value even though macOS output was not muted.
- `AVAudioPlayerNode.volume` was already `1.0`; application gain was not holding the system-volume indicator down.
- In the installed standalone Simulator, use **I/O > Increase Volume** and **I/O > Audio Output > System**. Apple documentation may show the equivalent Device Hub path as **Device > Sound**.
- Volume-button events must be spaced during automation; rapid events can be coalesced.
- `MPVolumeView` cannot change volume or choose routes in Simulator. Those controls must be validated on a physical iPhone.

### A live route change stops the engine

Changing the Simulator output while Cozy Focus was playing produced this Core Audio event:

```text
AVAudioEngine: iounit configuration changed > stopping the engine
```

The focus timer continued, but the audio engine remained stopped because the app did not observe and recover from engine-configuration, route-change, or interruption notifications.

The iOS player must:

1. Observe `AVAudioEngineConfigurationChange`.
2. Observe `AVAudioSession.routeChangeNotification`.
3. Observe `AVAudioSession.interruptionNotification`.
4. If focus is still active and resumption is appropriate, reactivate the session, reconnect/reschedule the retained loop buffer, restart the engine, and restart the player node.
5. Pause rather than automatically expose audio when headphones are disconnected, following Apple's route-change guidance.

### The 30-second loop itself was healthy

A 33.32-second host capture through BlackHole crossed the source-file boundary. Measured mean volume was `-31.5 dB` in all three windows:

- First five seconds
- Seconds 26 through 30
- Seconds 30 through 33

No silence longer than 250 ms was detected. `AVAudioPlayerNode.BufferOptions.loops` therefore rendered beyond the source duration correctly when the route stayed stable.

### The ambience assets are intentionally quiet

The ambience files are mastered much more quietly than the binaural tracks. Examples:

| Asset | Mean | Peak |
|---|---:|---:|
| Waterfall | -31.7 dB | -18.0 dB |
| Rainfall | -29.7 dB | -16.0 dB |
| Ocean waves | -34.3 dB | -17.3 dB |
| Black noise | -33.9 dB | -21.2 dB |
| Binaural analytical | -18.9 dB | -9.7 dB |

System volume and content loudness are separate controls. Raising system volume cannot make these tracks match the perceived loudness of the binaural tracks. Normalize the library or add a bounded gain stage with headroom and peak protection; do not rely on setting a player volume above its documented range.

### Choose the audio-session category deliberately

The app currently uses `.ambient`. Apple defines that as nonprimary audio that mixes with other apps and is silenced by the Ring/Silent switch or screen locking. If continuous focus audio is considered central to the experience, evaluate `.playback` instead. Any category change must preserve the desired mixing and background behavior and must be verified on a signed physical device.

## Android emulator follow-up

When implementing the equivalent Android recovery, verify:

- Audio focus gain/loss and whether playback should duck, pause, or resume.
- `AudioDeviceCallback` or the relevant Media3/AudioTrack route-change signal.
- Player recovery when the emulator audio sink or host output changes.
- Seamless looping across 30 seconds using a captured waveform, not only player state.
- Emulator media volume independently from macOS host volume.
- Loudness consistency across all copied `res/raw` assets.
- Bluetooth/headset disconnect privacy behavior on a physical Android device.

## Reproduction and evidence commands

```sh
# Inspect Simulator audio lifecycle events.
xcrun simctl spawn booted log show --last 10m --style compact \
  --predicate 'process == "CozyFocus" AND subsystem CONTAINS[c] "audio"'

# Inspect asset duration and loudness.
ffprobe -v error -show_entries format=duration -of csv=p=0 \
  CozyFocus/Resources/Sounds/waterfall.m4a
ffmpeg -hide_banner -i CozyFocus/Resources/Sounds/waterfall.m4a \
  -af volumedetect -f null -
```

Build/test success proves compilation and state behavior, not audible output. Simulator capture qualifies the simulator route only; final audio acceptance requires physical iOS and Android devices.
