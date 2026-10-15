# My Launcher

A lightweight, customizable Android home-screen launcher built in Java for the `DS-ann/PhoneBridge` project.

My Launcher is designed around a clean dark interface, configurable home-screen layout, practical shortcuts and widgets, and a custom page-transition effect. The project avoids third-party UI frameworks and uses Android's native view system.

> **Branch:** `launcher-app` · **Package:** `com.dsann.launcher` · **Minimum Android:** Android 6.0 (API 23)

## Highlights

- **Home screen and app drawer** — browse installed launchable apps, open them, and use long-press actions.
- **Multiple home pages** — organize shortcuts across pages and navigate with horizontal swipes.
- **3D wheel-style page transition** — the active page pivots around a vertical edge as the adjacent page comes into view.
- **Home Studio customization** — adjust layout and appearance, including clock size and position, page rotation, animation speed, icon sizing, grid columns, labels, and other available display options.
- **Clock and date** — configurable clock presentation, 12/24-hour format, and optional date display.
- **App shortcuts and dock** — pin apps to the home screen, organize shortcuts, and keep frequently used apps in the dock.
- **Contact / direct-call shortcut support** — create a contact shortcut and configure its home-screen placement and size. Android may request call permission before placing a call.
- **Android app widgets** — browse available widget providers, add widgets, and interact with supported widgets from the home screen.
- **Wallpaper and theme customization** — use the available wallpaper picker and launcher color options.
- **App management actions** — open App info, uninstall eligible apps, add or remove home-screen shortcuts, and organize items through the launcher menus.
- **App drawer performance work** — cache app labels and icons, reuse grid cells, and refresh the app list when Android reports an installed-app change instead of reloading it on every drawer opening.

The exact options visible in Home Studio can evolve as the launcher is updated.

## Compatibility

| Item | Details |
| --- | --- |
| Minimum Android version | Android 6.0 (API 23) |
| Compile SDK | Android SDK 35 |
| Language | Java |
| UI | Native Android views |
| Application ID | `com.dsann.launcher` |

The launcher is intended to run on Android 6.0 and later. Android versions and device manufacturers can differ in how they handle widgets, wallpaper selection, call permissions, and default-home-app settings; these behaviors should be tested on the target device.

## Install the APK

### Download a build from GitHub Actions

1. Open the [Build Launcher APK workflow](https://github.com/DS-ann/PhoneBridge/actions/workflows/build-launcher.yml).
2. Select a **successful** run for the `launcher-app` branch.
3. In the run's **Artifacts** section, download **My-Launcher-release-signed-APK**.
4. Extract the ZIP and install the APK on the Android device.

If Android blocks installation, allow installation from the app you used to open the APK, where required by your Android version and device settings.

### Set My Launcher as the default home app

1. Install and open **My Launcher**.
2. Press the device's Home button.
3. If Android displays a home-app chooser, select **My Launcher** and choose the desired default option.
4. If no chooser appears, open Android's default-app or Home-app settings and select **My Launcher**.

Menu names vary between Android versions and manufacturers.

## Build with GitHub Actions

The repository includes a workflow at [`.github/workflows/build-launcher.yml`](.github/workflows/build-launcher.yml).

- Runs on pushes to `launcher-app` and can be started manually with **Run workflow**.
- Uses JDK 17, Gradle 8.7, and Android Build Tools 35.0.0.
- Builds the release APK and verifies its signature before publishing the artifact.
- Uploads the signed APK as **My-Launcher-release-signed-APK**.
- Artifacts are retained for 14 days.

The release workflow requires the repository's configured signing secrets for the existing PKCS#12 keystore. The secret values must remain in GitHub Actions Secrets; do not commit keystores, passwords, signing keys, or generated APKs to the repository.

A successful workflow confirms that the release APK was built and its signature verified. It does **not** prove that every gesture, widget provider, call flow, or device-specific behavior works correctly at runtime.

## Project structure

```text
app/
└── src/main/
    ├── AndroidManifest.xml
    ├── java/com/dsann/launcher/
    │   ├── MainActivity.java
    │   ├── LauncherAppAdapter.java
    │   ├── HomePageController.java
    │   ├── WidgetInteractionController.java
    │   └── LauncherUiUtils.java
    └── res/values/
.github/
└── workflows/
    └── build-launcher.yml
```

Key files:

- **`MainActivity.java`** — launcher UI, home and drawer behavior, gestures, clock, customization, app shortcuts, and widget integration.
- **`LauncherAppAdapter.java`** — home/drawer app-cell rendering, view reuse, and label/icon caching.
- **`HomePageController.java`** — home-page organization and page-related behavior.
- **`WidgetInteractionController.java`** — widget interactions and editing behavior.
- **`LauncherUiUtils.java`** — shared UI helpers.
- **`AndroidManifest.xml`** — launcher entry points, Home intent filter, and declared permissions.
- **`app/build.gradle`** — Android SDK levels, app identity, and Java compatibility.
- **`.github/workflows/build-launcher.yml`** — automated release build, signing verification, and artifact upload.

## Development notes

- Keep the minimum SDK at **23** unless Android 6.0 support is intentionally dropped.
- Prefer native Android APIs and avoid unnecessary background work or heavyweight UI dependencies.
- When changing gestures, verify that app dragging, widget interaction, contact-shortcut resizing, page swipes, and opening the app drawer do not interfere with one another.
- When changing layout or clock sizing, verify that the bottom home-screen row remains visible at the largest supported clock size.
- After changes, check the GitHub Actions run and test the installed APK on real target devices. CI is not a substitute for runtime testing.

## Current testing status

This repository's automated workflow builds and verifies the signed release APK. Runtime quality depends on testing the resulting build on actual devices, especially for gesture smoothness, resizing, widgets, direct-call shortcuts, wallpaper selection, and Android-version differences. Do not interpret a green build alone as confirmation that these interactions have been device-tested.

## License

No license is specified in this branch's README. Unless a license file is added, reuse and redistribution permissions should not be assumed.
