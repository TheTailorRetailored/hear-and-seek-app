<p align="center">
  <img src="docs/images/social-preview.png" alt="Hear & Seek — hide the phone and follow the sound" width="100%">
</p>

# Hear & Seek

Hear & Seek is an Android hide-and-seek game. One player hides the phone while it is silent. The phone then plays a continuous tone that falls from 20 kHz to 800 Hz, so players with younger ears may get a head start and everyone else gradually joins the hunt.

<p align="center">
  <img src="docs/images/home-screen.png" alt="Hear & Seek home screen with Quick, Classic, Long and Marathon modes" width="320">
</p>

## Game modes

- **Quick:** 45-second sweep, 10-second hiding countdown
- **Classic:** 90-second sweep, 15-second hiding countdown
- **Long:** 3-minute sweep, 20-second hiding countdown
- **Marathon:** 5-minute sweep, 30-second hiding countdown

Players can tap **Found It!** during the hunt. If the sweep finishes first, the app asks whether the phone was found and records a found/not-found outcome for the round.

## Build

The project uses Kotlin, Jetpack Compose, Android Gradle Plugin 8.13.2, Gradle 8.13 and JDK 17.

```bash
gradle testDebugUnitTest assembleDebug
```

The APK is created at:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Every pull request and push to `main` runs Android CI and uploads the debug APK as a private workflow artifact.
