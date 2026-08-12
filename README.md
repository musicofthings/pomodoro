# Cozy Focus

A light-only, offline-first SwiftUI Pomodoro app designed to make focus feel lower pressure.

Open `CozyFocus.xcodeproj` in Xcode, select an iOS 17+ device or simulator, and run the `CozyFocus` scheme.

## Included

- One-tap, 25-minute focus timer that correctly reconciles elapsed time after returning to the app.
- Six calming companions with a subtle breathing loop and unlockable cosmetic items.
- SwiftData-backed focus history and device-local coin/inventory persistence.
- Optional soft five-minute haptic anchors and locally generated rain/lo-fi ambient loops.
- Optional Screen Time app/category shielding using FamilyControls and ManagedSettings.
- A 1920×1080 share card rendered locally for the standard iOS share sheet.

## Device note

Screen Time shielding needs the Family Controls capability enabled for the selected App ID and a development/distribution provisioning profile that contains it. The simulator build validates the code path but cannot qualify authorization or shielding behavior on a physical device.

If the app is interrupted while distractions are shielded, it clears the app's prior shielding settings on its next launch rather than leaving them active without a live focus session.

`Synced Growth Farm` is a separate iPadOS multiplayer product from the supplied brief. It needs its own Cloudflare Durable Objects/D1 backend and is intentionally not represented as a partial feature in this single-player iOS app.
