# Cozy Focus

Cozy Focus is an offline-first Pomodoro app for iOS and Android. Both apps keep focus history, coins, inventory, preferences, and active-timer recovery on the device; no account or backend is required.

## iOS

Open `CozyFocus.xcodeproj` in Xcode 26.6 or newer, select an iOS 17+ device or simulator, and run the `CozyFocus` scheme.

The iOS app includes:

- A deadline-backed timer that reconciles elapsed time after suspension or relaunch.
- SwiftData-backed focus history, coin ledger, and cosmetic inventory.
- Local completion notifications, ambient audio, haptics, companion animations, and local share-card rendering.
- Optional Screen Time shielding through Family Controls, Managed Settings, and the `CozyFocusShieldMonitor` Device Activity extension.

Screen Time requires the Family Controls capability for both the app and extension App IDs, App Group `group.com.cozyfocus.app`, approved distribution entitlement access, and matching provisioning profiles. Simulator or unsigned builds cannot qualify authorization, shielding, or extension callbacks; test those behaviors on a signed physical device.

## Android

Open `CozyFocusAndroid/` in a current Android Studio release, or build with the checked-in Gradle wrapper and JDK 17:

```sh
cd CozyFocusAndroid
./gradlew testDebugUnitTest compileDebugAndroidTestKotlin lintDebug assembleDebug bundleRelease
```

The Android app compiles against and targets API 37, supports API 24+, and uses current stable Android tooling as of August 2026: AGP 9.3.1, Gradle 9.7.0, Kotlin 2.4.10, Compose BOM 2026.08.00, Room 2.8.4 with KSP, and DataStore 1.2.1.

Android behavior and platform boundaries:

- Room persists focus history, coin ledger, and inventory; session rewards and purchases are atomic and duplicate-safe.
- DataStore persists the timer deadline, active-session identity, pause state, companion, cosmetic, ambient sound, and notification-prompt state.
- Exact alarms drive completion for this timer app, with an inexact fallback where exact alarm access is unavailable. Android 13+ also requires notification permission before completion alerts can appear.
- The Focus button opens Android's user-controlled Focus/Do Not Disturb settings. Cozy Focus does not claim to block other apps through an unsupported or hidden API.
- Android backup is disabled so the local focus library is not copied to cloud backup. Uninstalling the app deletes its local data.
- Instrumented Room and Compose tests require an Android device or emulator.

The Android application ID is `com.cozyfocus.app`. A Play release additionally requires a private signing key, Play App Signing configuration, and an exact-alarm permission declaration because precise timer completion is core user-facing functionality.

## Validation

Run the iOS simulator tests with:

```sh
xcodebuild test \
  -project CozyFocus.xcodeproj \
  -scheme CozyFocus \
  -destination 'platform=iOS Simulator,name=iPhone 17,OS=latest' \
  CODE_SIGNING_ALLOWED=NO CODE_SIGNING_REQUIRED=NO
```

Compilation and simulator tests are not substitutes for signed-device testing of Family Controls, Managed Settings, Device Activity, Android exact alarms, notification permission, OEM battery restrictions, audio, haptics, or sharing.

`Synced Growth Farm` is a separate iPadOS multiplayer product from the supplied brief. It requires its own backend and is intentionally not represented as a partial feature in this local-first app.
