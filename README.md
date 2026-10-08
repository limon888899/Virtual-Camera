# Live Media Assistant (Virtual Camera)

An Android app for local video playback, floating playback controls, microphone analysis, and an audio-reactive avatar.

## Features

- Import videos from Android's media picker; the app stores playlist entries and playback preferences locally.
- Preview imported videos in the app or in the optional floating player. Playback speed, repeat-one, repeat-all, play-once, mirror, play/pause, and next-video controls apply to these local players.
- Monitor microphone input inside the app, select simple local audio filters, and optionally listen to the processed signal in the app. Headphones are strongly recommended to avoid acoustic feedback.
- Animate a selected photo avatar from microphone level and basic signal analysis.
- Check Android permissions and, on supported devices, request an Android-managed work profile for a separate app installation.

## Android platform boundaries

This is an ordinary Android application. Android does not expose a public API for a third-party app to replace the system camera feed or another app's microphone input. The preview and processed-audio monitor work only inside this app; they do not feed calls, meetings, or other apps.

During an imo (or other) live video call, grant “Display over other apps” and show the floating player to watch a selected gallery video over the call on your own screen. This overlay does not replace imo's camera: the other participant continues to receive the phone's normal camera feed.

Lip-sync is a local, audio-reactive avatar animation. The app does not perform AI face-landmark tracking, generate/export lip-synced video, or provide system-wide virtual-camera output. Noise filtering uses an optional device `NoiseSuppressor` and a lightweight local filter; it is not an AI noise model. Work-profile creation is subject to device-owner support, Android version, and user consent.

## Build and test

Prerequisites: Android Studio or a Java 11-compatible Android build environment with the Android SDK required by the project.

```sh
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug
```

The installable debug APK is created at `app/build/outputs/apk/debug/app-debug.apk`. It is signed with the project's debug key for direct testing and is not a Play Store release.

A copy of the tested APK is also included at [`release/Live-Media-Assistant-debug.apk`](release/Live-Media-Assistant-debug.apk). Install it manually on an Android device; allow installation from the app used to open the APK if Android asks. This debug-signed build cannot update an installation signed with a different key.

For a production release, configure a private upload key outside the repository using `KEYSTORE_PATH`, `STORE_PASSWORD`, and `KEY_PASSWORD`, with the expected `upload` key alias, then run:

```sh
./gradlew :app:assembleRelease
```

Never commit a keystore or signing passwords. `GEMINI_API_KEY` is not required by the current app features.

## Permissions

Microphone access is requested only when enabling local audio monitoring. The video player and optional floating player can run without microphone access. Display-over-other-apps permission is optional and is required only for the floating player. Android may limit background service behavior based on user settings and device policy.
