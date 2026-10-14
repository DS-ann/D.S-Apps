# My Launcher

Android home-screen launcher maintained by Debarghya Sannigrahi.

## Features

- Home-screen shortcuts and an app drawer opened by swiping up.
- Additional home pages when a page reaches its shortcut capacity.
- Horizontal swipe navigation with a page transition animation.
- 12-hour clock and optional date display.
- Home customization: grid columns, icon size, date visibility, and custom wallpaper.
- Long-press app actions: App info, Uninstall, Add to Home, Remove from Home, and shortcut ordering.
- Android 6.0 (API 23) minimum; Java implementation.

## Build

GitHub Actions builds a debug APK and a signed release APK when changes are pushed to `launcher-app`.

1. Open the [Build Launcher APK workflow](https://github.com/DS-ann/PhoneBridge/actions/workflows/build-launcher.yml).
2. Select the latest successful run.
3. Download **My-Launcher-debug-APK** for the debug build or **My-Launcher-release-signed-APK** for the signed release.
4. Extract the downloaded artifact ZIP and install the APK.

To use it as the device home screen, open Android's default Home app settings and select **My Launcher**.

## Project files

- `app/src/main/java/com/dsann/launcher/MainActivity.java` — launcher behavior and UI.
- `app/src/main/AndroidManifest.xml` — app and Home intent filters.
- `app/src/main/res/values/` — app label and theme.
- `.github/workflows/build-launcher.yml` — build, signing verification, and APK artifact upload.

The CI build verifies compilation and release APK signing. It does not replace testing on a physical device; gesture behavior and device-specific wallpaper picker behavior still require runtime verification.
