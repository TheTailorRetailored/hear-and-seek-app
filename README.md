# Hear & Seek

Hear & Seek is a tiny Android hide-and-seek game. One player hides the phone while it is silent. The phone then plays a continuous tone that falls from 20 kHz to 800 Hz over 90 seconds, so players with younger ears may get a head start and everyone else gradually joins the hunt.

## MVP flow

1. Tap **Start Game**.
2. Hide the phone during the 15-second countdown.
3. The tone begins automatically and descends for 90 seconds.
4. Tap **Found It!** when someone locates the phone.

## Build

The project uses Kotlin, Jetpack Compose, Android Gradle Plugin 8.13.2, Gradle 8.13 and JDK 17.

Open the project in Android Studio, or use an installed Gradle 8.13:

```bash
gradle testDebugUnitTest assembleDebug
```

The APK is created at:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Every pull request and push to `main` also runs the Android CI workflow and uploads the debug APK as a private workflow artifact.
