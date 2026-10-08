# Civora runtime demo

These are genuine ADB captures of the Android application on Pixel_8, Android 15 (API 35), on 8 October 2026. Campus information and accounts are fictional, hosted only by the local `demo-civora` Firebase emulators. Integration-test fixtures also remain in this disposable emulator database.

## Start the local demo on Windows

From the repository root, run in one PowerShell terminal:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
$env:FUNCTIONS_DISCOVERY_TIMEOUT = '60'
firebase emulators:start --project demo-civora --only 'auth,firestore,functions'
```

Android Studio's Java runtime successfully started Firestore in this environment. The initial system-Java launch exited unexpectedly. Functions startup exceeded the default discovery deadline; the larger deadline is documented in [Firebase initialization guidance](https://firebase.google.com/docs/functions/tips#avoid_deployment_timeouts_during_initialization).

In another terminal:

```powershell
$env:FIREBASE_AUTH_EMULATOR_HOST = '127.0.0.1:9099'
$env:FIRESTORE_EMULATOR_HOST = '127.0.0.1:8080'
node functions/scripts/seed-demo.js
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
& $adb reverse tcp:9099 tcp:9099
& $adb reverse tcp:8080 tcp:8080
& $adb reverse tcp:5001 tcp:5001
.\gradlew.bat assembleDebug '-Pcivora.emulatorHost=127.0.0.1'
& $adb install -r app/build/outputs/apk/debug/app-debug.apk
& $adb shell am start -n com.sachinkanna.civora/.MainActivity
```

ADB forwarding worked here; `10.0.2.2` could not reach the host. Quote the Gradle property and comma-separated Firebase service list in PowerShell.

Local accounts: `student`, `faculty`, `admin`, `vendor`, and `driver` at `demo.example.com`, each with password `DemoCampus123!`. These are public emulator fixtures, never production credentials. The seed script refuses non-loopback endpoints and creates existing documents only when absent. It does not reset tickets, orders, reports, trip states, or GPS. Run it before the app; do not run rules tests concurrently with a demo because rules tests clear their emulator database.

## Artifacts

- `screenshots/`: actual runtime screenshots, including student, admin, vendor, and driver screens.
- `Civora-Demo.mp4`: local ADB recording of representative student navigation and food ordering; not a complete end-to-end recording.
- `apk/Civora-emulator-debug.apk`: local demo build requiring emulators and ADB forwarding.
- `apk/Civora-live-debug.apk`: debug build targeting the configured Firebase project; live services still require setup.
- `../app/build/outputs/apk/debug/app-debug.apk`: latest Gradle build output.

APKs, video, and emulator exports are ignored by Git. Captures and instructions may be committed. No GPS readings are seeded. Bus captures show unavailable live location/ETA. Push registration is skipped for the demo project because FCM is not emulated.

Use `python demo/capture.py list`, `tap 'visible label'`, and `capture filename.png` to navigate and capture through ADB. Capture refuses to save another application's screen as Civora. UI commands should run sequentially.

For security tests with a fresh emulator session:

```powershell
$env:FUNCTIONS_DISCOVERY_TIMEOUT = '60'
firebase emulators:exec --project demo-civora --only 'auth,firestore,functions' 'npm --prefix functions test'
```

See [the release report](../docs/LIVE_DEPLOYMENT_REPORT.md) for verified coverage and remaining production blockers.
