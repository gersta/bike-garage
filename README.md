# Bike Garage

Lokale Android-App zur Verwaltung einer Fahrradgarage.

## Voraussetzungen und Build

- JDK 17 oder neuer
- Android SDK Platform 37 und Build Tools 37.0.0

```sh
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
./gradlew :app:lintDebug
```

Die App unterstützt Android 12 (API 31) und höher. Sie verwendet ausschließlich
lokale Speicherung; Auto Backup und die `INTERNET`-Berechtigung sind deaktiviert.

Instrumentierte Compose-Tests benötigen einen laufenden API-31-Emulator:

```sh
./gradlew :app:connectedDebugAndroidTest
```

Für eine vollständig projektlokale SDK-/AVD-Installation können `.android-sdk/`
und `.android-home/` verwendet werden. Beide Verzeichnisse und `local.properties`
sind absichtlich nicht versioniert.
