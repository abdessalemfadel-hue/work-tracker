# Work Tracker

A private, offline-first Android work-hours tracker rebuilt with Kotlin, Jetpack Compose and Material 3.

## What it does

- Clock in, clock out, start breaks and resume work.
- Full-screen animated confirmation ripples with haptic feedback.
- Persistent foreground notification with live shift status and lock-screen actions.
- Notification actions for Break, Resume and Clock out.
- Daily, weekly and monthly analytics.
- Overtime, average hours, longest day, shortest day, average arrival and average finish statistics.
- Searchable shift history with add, edit, duplicate and delete actions.
- Monthly calendar with normal, overtime, active and no-work states.
- CSV reports and JSON backup/restore through the Android Storage Access Framework.
- Salary and effective hourly-rate estimates.
- Configurable scheduled hours, expected breaks, reminders, theme and dynamic color.
- Home-screen Glance widget.
- Quick Settings tile for clocking in and out.
- Boot recovery for unfinished shifts.
- WorkManager checks for target-hour and long-shift reminders.

## Privacy

Work Tracker has no internet permission, advertising, analytics SDK, account system, location access, microphone access or contact access. Data remains in the local Room database unless the user explicitly exports it.

## Architecture

- Kotlin
- Jetpack Compose and Material 3
- Navigation Compose
- Room database
- DataStore Preferences
- Hilt dependency injection
- ViewModel, StateFlow and Kotlin Coroutines
- Foreground service and NotificationCompat
- WorkManager
- Glance AppWidget

The app uses Room as the single source of truth for shift records. ViewModels expose immutable StateFlow UI state. The foreground service reads the same repository and survives normal process recreation.

## Build locally

Requirements: JDK 17 and Android SDK 35.

```bash
chmod +x ./gradlew
./gradlew testDebugUnitTest
./gradlew lintDebug
./gradlew assembleDebug
```

APK output:

```text
app/build/outputs/apk/debug/app-debug.apk
```

The repository includes a Gradle bootstrap script. On first use it downloads Gradle 8.7 into `.gradle-bootstrap`.

## GitHub Actions

Every push and pull request runs unit tests, Android lint and a debug APK build. Download the APK from:

1. Open **Actions**.
2. Open the latest successful **Build Android APK** run.
3. Scroll to **Artifacts**.
4. Download **WorkTracker-debug-apk**.
5. Extract the ZIP and install `app-debug.apk`.

## Samsung limitation

Samsung does not expose a general public API that lets ordinary third-party apps insert content directly into Now Brief. Work Tracker provides the supported alternatives: a public lock-screen notification, expanded notification actions, a home-screen widget and a Quick Settings tile.

## Screenshots

Add current screenshots to `docs/screenshots/` after installing the APK:

- Dashboard and active timer
- Clock-in ripple animation
- Persistent notification
- History editor
- Analytics
- Calendar
- Reports and salary settings

## Roadmap

- Multiple break timeline entries instead of an accumulated break duration.
- Optional encrypted backup files.
- PDF report generation.
- Biometric app lock.
- Configurable custom date-range analytics.
- More widget sizes and richer widget controls.
