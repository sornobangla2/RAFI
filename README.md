# Pikachu Safe Lab (Android)

A native Android, offline-only simulation app inspired by the supplied UI. It accepts fake identifiers in the `TEST-...` format and counts up to 100 mock events locally.

- No `INTERNET` permission and no HTTP client.
- Rejects phone-number input; accepts only `TEST-[A-Z0-9]{1,12}` IDs.
- Simulation history stays in memory and is cleared when the app closes.
- No credentials or third-party OTP endpoints are included.

## Open and build

Open this folder in Android Studio, allow Gradle sync, then run the `app` configuration on an emulator or device. The debug APK is produced under `app/build/outputs/apk/debug/`.

Build configuration: Android Gradle Plugin 9.2.1, Gradle 9.4.1, compile/target SDK 36, Java 17.