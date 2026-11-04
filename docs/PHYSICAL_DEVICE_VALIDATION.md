# Civora physical Android device validation

Date: 12 October 2026 (Asia/Kolkata)  
Source: `ab162e854fa14ea2055de89cd51132cc1cac0452` on `feat/civora-production-completion`  
Scope: non-destructive validation only; no production credentials, production data, deployment, signing, or institutional approval.

## Current result

No authorized physical Android device was connected. ADB returned only the existing Android emulator, so no physical installation or handset UI/API result is claimed.

| Check | Status | Evidence |
|---|---|---|
| Source baseline | PASS | `git rev-parse HEAD` returned `ab162e854fa14ea2055de89cd51132cc1cac0452`; branch was one local commit ahead before this report. |
| Existing debug APK | PASS | `app/build/outputs/apk/debug/app-debug.apk`, 29,618,817 bytes, SHA-256 `72F6B8FA8BF1B6122576B2147C085C7AB043F4B6B769E7324B0A4A7D07928DE3`. |
| APK identity and SDK range | PASS | Package `com.sachinkanna.civora`, version 2 / 2.0, min API 26, target API 36. |
| APK signature | PASS | `apksigner` verifies APK Signature Scheme v2 with the standard Android Debug certificate. This is development signing only. |
| ADB availability | PASS | SDK ADB 1.0.41 / platform-tools 37.0.0 is available at `%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe`. It is not currently on `PATH`. |
| Authorized physical device | BLOCKED | `adb devices -l` returned only `emulator-5554`, model `sdk_gphone64_x86_64`. No hardware serial was present. |
| Install debug APK on physical device | BLOCKED | Requires an authorized physical target. The existing APK was not installed on the emulator as a substitute for this gate. |
| Authentication smoke test | BLOCKED | Requires the physical target plus disposable local-emulator accounts. No real institutional credentials are authorized. |
| Navigation smoke test | BLOCKED | Requires the physical target. |
| Staff-transfer workflow | BLOCKED | Requires the physical target and disposable emulator fixtures. Validation must stop after preview/field guards; do not deactivate, transfer, or delete an account. |
| Network/API integration | BLOCKED | Requires a physical target connected to isolated local Firebase emulators. The existing APK embeds `10.0.2.2`, which is Android-emulator-specific and is not a valid physical-device host configuration. |
| Error handling | BLOCKED | Requires the physical target and controlled offline/unreachable-emulator conditions. |

## Windows device setup

1. On a supported Android phone (Android 8/API 26 or newer), enable Developer options and USB debugging. Use a data-capable USB cable, unlock the phone, and keep its screen visible.
2. Connect the phone directly to the Windows computer. On the phone, review the USB-debugging fingerprint and approve it only for this computer. This approval must be performed by the device owner.
3. In PowerShell at the repository root, use the SDK-bundled ADB explicitly:

   ```powershell
   $adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
   & $adb kill-server
   & $adb start-server
   & $adb devices -l
   ```

4. Continue only when the hardware serial is listed as `device`. Resolve these states before testing:

   - `unauthorized`: unlock the phone and accept the RSA prompt; reconnect if the prompt is absent.
   - `offline`: reconnect the cable, change the USB mode to file transfer, then restart ADB.
   - no hardware row: try another data cable/USB port and install the phone manufacturer's Windows USB driver if Device Manager does not identify the handset correctly.
   - multiple targets: pass `-s <physical-serial>` to every ADB command. Never select `emulator-5554` for physical evidence.

## Isolated physical-device build and installation

The checked APK targets `10.0.2.2`, so it may be installed for launch/layout inspection but cannot validly prove physical-device API integration. For complete isolated integration, start the local Firebase suite, seed only the fictional demo project, use ADB reverse, and build a debug APK whose emulator host is device loopback:

```powershell
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
$serial = '<physical-serial-from-adb-devices>'
$env:FUNCTIONS_DISCOVERY_TIMEOUT = '120'

# Terminal 1, from the repository root:
npx -y firebase-tools@14.24.2 emulators:start --project demo-civora --only 'auth,firestore,functions,storage'

# Terminal 2, after all emulators report ready:
$env:FIREBASE_AUTH_EMULATOR_HOST = '127.0.0.1:9099'
$env:FIRESTORE_EMULATOR_HOST = '127.0.0.1:8080'
node functions/scripts/seed-demo.js
& $adb -s $serial reverse tcp:9099 tcp:9099
& $adb -s $serial reverse tcp:8080 tcp:8080
& $adb -s $serial reverse tcp:5001 tcp:5001
& $adb -s $serial reverse tcp:9199 tcp:9199
.\gradlew.bat assembleDebug '-Pcivora.emulatorHost=127.0.0.1' --max-workers=2
& $adb -s $serial install -r app/build/outputs/apk/debug/app-debug.apk
& $adb -s $serial shell am start -n com.sachinkanna.civora/.MainActivity
```

Use only the fictional accounts created by `functions/scripts/seed-demo.js`. Do not run the backend/rules test suite concurrently; it clears the disposable emulator database. The rebuilt APK remains debug-signed and must not be distributed as a release.

To install the already-built APK for launch-only inspection instead, use:

```powershell
& $adb -s $serial install -r app/build/outputs/apk/debug/app-debug.apk
```

Record that API integration is BLOCKED for that artifact because its embedded host is `10.0.2.2`.

## Manual non-destructive validation checklist

Record the phone manufacturer/model, Android version/API, hardware serial suffix only (last four characters), test APK SHA-256, start/end time, and screenshots or screen recording with no personal data.

| Area | Procedure | PASS criterion |
|---|---|---|
| Clean launch | Force-stop and launch Civora twice; rotate once if supported. | No crash/ANR; onboarding or sign-in is legible and respects system insets. |
| Authentication guard | Submit empty and intentionally invalid fictional credentials, then sign in with a disposable seeded student account. | Clear validation/error text; no privileged-role selector; valid demo account reaches the correct role shell. |
| Navigation | Visit every bottom-level student destination, open a detail page, use the visible app Back control, background for 30 seconds, and resume. | Correct destination highlighting, no blank/duplicate screen, Back returns once, state remains coherent. |
| Staff access navigation | Sign in with the disposable seeded admin, open **Staff access**, verify target/replacement fields and confirmation guards, and run **Review responsibilities** for a disposable active staff fixture. | Preview succeeds and displays counts; transfer remains disabled until required inputs and phrase are present. Do **not** press **Transfer and deactivate** or **Permanently delete account**. |
| API integration | With ADB reverse and local emulators running, refresh Today and another data-backed screen; inspect host emulator logs. | Fictional seeded content loads and the request appears only in `demo-civora` local emulator logs. No production endpoint or data is used. |
| Offline handling | While on a loaded screen, enable airplane mode manually, refresh/navigate, then disable airplane mode and retry. | App remains responsive, shows understandable failure/loading state, and recovers without duplicate destructive action. |
| Unreachable backend | Stop the local emulators, relaunch/sign in with fictional data, then restart them and retry. | Controlled error instead of crash/ANR; recovery succeeds after services return. |
| Permission denial | Visit camera/location-dependent screens and deny permission when prompted. | Feature explains or safely disables unavailable behavior; navigation remains usable. Do not test real moving GPS in this pass. |
| Process recovery | Background Civora, let Android reclaim it if practical, then reopen from Recents. | No crash, stale privileged screen, or unintended action replay. Record BLOCKED if process death cannot be induced non-destructively. |
| Logs | During each scenario run `& $adb -s $serial logcat -c`, reproduce once, then export `& $adb -s $serial logcat -d -v threadtime > docs/evidence/physical-device-<scenario>.log`. | No Civora fatal exception or ANR. Logs stay local/ignored and are reviewed for tokens, email addresses, or personal data before sharing. |

## Outstanding blockers

- An authorized physical Android handset is not connected.
- Physical authentication, navigation, staff-preview, local API, offline recovery, permission denial, process recovery, camera/QR, moving GPS, battery, and performance evidence has not been produced.
- Real FCM, Play Integrity/App Check, Crashlytics receipt, production indexes/rules, release signing, Play upload, and production deployment remain outside this local pass.
- Institutional credentials, production data, institutional approval, signing keys, deployment authorization, and publication authorization have not been supplied or inferred.
