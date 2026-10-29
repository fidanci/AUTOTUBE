# AutoTube Android

AutoTube is an Android app with an in-app YouTube shortcut, a web browser, and user-managed website shortcuts.

## Download and updates

GitHub Actions builds the app after each change to `main` and publishes an APK release. In AutoTube, tap **Güncellemeleri kontrol et** to check the latest release, download it, and open Android's installer. Android may ask you to allow AutoTube to install updates from this source once.

The APK is a sideload/debug build for this personal project. GitHub Actions keeps its signing key in the repository's Actions cache so releases can update the same installation. Do not use this signing arrangement for a public production app.

## Build

The project uses Android Gradle Plugin 9.4.1, Gradle 9.7.1, JDK 17, and Android SDK 36.
