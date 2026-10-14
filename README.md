# My Launcher

A lightweight Android home-screen launcher starter project by **Debarghya Sannigrahi**.

## Current features

- Home-screen launcher entry; select My Launcher as the default Home app in system settings.
- App grid populated from launchable apps installed on the device.
- Tap an app icon to launch it.
- Dark, simple interface with no third-party runtime dependencies.

## Compatibility

- Minimum Android version: Android 6.0 (API 23)
- Target SDK: API 28
- Language: Java
- Build: Android Gradle Plugin 8.6.1, Gradle 8.7, JDK 17

The build tools run on GitHub Actions with JDK 17; the APK declares minSdk 23, so it is intended to install on Android 6.0 and newer.

## Build APK on GitHub

1. Open the Actions tab.
2. Select Build Launcher APK.
3. Push a commit to launcher-app or run the workflow manually with Run workflow.
4. Open the successful workflow run and download the My-Launcher-debug-APK artifact.
5. Extract the ZIP to get app-debug.apk.

## Install and test

Install the APK, then open Android's Home app settings and choose My Launcher as the default Home app. Use the device's Home button to test it.

## Project layout

- app/src/main/java/com/dsann/launcher/MainActivity.java — launcher UI and app launching.
- app/src/main/AndroidManifest.xml — launcher/Home intent filters and app visibility query.
- app/build.gradle, build.gradle, settings.gradle — Android build configuration.
- .github/workflows/build-launcher.yml — builds and uploads the debug APK.

## Notes

This is an initial functional launcher foundation, not a polished production launcher. Widgets, wallpaper picker, search, favorites, and customization can be added later.
