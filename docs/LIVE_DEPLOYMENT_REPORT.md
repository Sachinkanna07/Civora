# CIVORA — LIVE DEPLOYMENT AND DEMO REPORT

Verified on 8 October 2026, Windows/PowerShell. Starting commit: `dba4fab`, branch `master`, clean working tree.

## Firebase

| Check | Verified result |
| --- | --- |
| Project | `civora-8f56e`; authenticated CLI account can access it |
| CLI / runtime | Firebase CLI 15.33.0; Node 22.23.2 matches Functions Node 22 |
| Android configuration | Local configuration exists and matches project/package; ignored by Git |
| Project selection | `firebase use civora-8f56e` succeeded; added persistent `.firebaserc` mapping |
| Permissions | CLI IAM checks returned database-list and function-list permissions |
| Authentication | Admin configuration endpoint returned HTTP 404 `CONFIGURATION_NOT_FOUND`; Email/Password is not verified enabled |
| Firestore | Database-list request returned HTTP 403 `SERVICE_DISABLED` |
| Functions | Function-list request returned HTTP 403 `SERVICE_DISABLED` |
| Billing | Read-only billing API check returned `billingEnabled: false` |
| Deployment | Not attempted: required backend setup and explicit billing approval are outstanding |
| Rules / indexes | Audited locally; rules passed emulator tests; neither deployed nor verified live |

No billing upgrade, production seeding, or production administrator promotion was performed. No production deployment success is claimed. Emulator execution loaded all 14 backend exports in `us-central1`; Android callable names match the backend exports. The emulator does not establish production composite-index readiness or push delivery.

## Fixes and validation

- Fixed login crash when demo Firebase credentials reached synchronous FCM initialization. Demo skips push registration; live push failures no longer crash login.
- Corrected debug-emulator placeholder API key/application ID syntax. Verified authenticated Android callable requests subsequently reached the emulator and created a QR registration and food orders.
- Made bus assignment validation/write transactional with the active-trip check, preventing assignment changes racing with trip start.
- Reject malformed top-level callable data and null cart entries with a controlled validation error.
- Hardened bootstrap: explicit project and existing Civora profile are required before claims are changed; the script verifies the resulting admin claim. Production execution still requires confirmed UID and trusted credentials. Auth and Firestore writes are separate operations, so a failure between them can require an idempotent retry.
- Added isolated, create-only demo seeding and genuine ADB capture tooling.
- Reset the driver's GPS status message when ending a trip; the completed-trip screen had retained "Waiting for GPS" after sharing stopped.
- Configure debug Firebase emulators once per process. A device theme change recreated the Activity and replaced Firebase beneath retained ViewModels, leaving attendance listeners stale; repeated Activity creation now preserves the configured Firebase app.

| Validation | Final result |
| --- | --- |
| `npm --prefix functions ci` | Exit 0; 292 packages installed; lockfile unchanged |
| Android unit tests | 14 passed, 0 failures |
| Debug assembly | Exit 0; both configured-project and emulator APKs built |
| Lint | Exit 0; 0 errors, 43 warnings, mainly dependency/version-catalog notices |
| Plain backend tests | 3 policy tests passed; 8 emulator-dependent checks skipped as designed |
| Emulator backend suite | 11 passed, 0 failed, 0 skipped: 3 policy, 7 rules, 1 multi-step integration scenario |
| Additional integration rerun | Passed after adding active-trip assignment protection checks |
| Targeted negative callable checks | Null cart entry returned `INVALID_ARGUMENT`; unauthenticated RSVP returned `UNAUTHENTICATED` |
| Device instrumentation | Both tests passed on Pixel_8, Android 15, exit 0; subsequent driver/theme fixes additionally verified in runtime |

Rules/integration coverage includes privileged-role forgery, another user's protected profile, direct workflow writes, event capacity race and duplicate RSVP, invalid/replayed attendance tickets, server menu pricing and idempotent orders, wrong-vendor order management, invalid transitions, wrong-driver trip management, GPS ownership/throttle/coordinate validation, report permissions, and private notification ownership. Negative tests intentionally generate permission-denied logs.

Initial integration failure was environmental: Functions discovery timed out and the test received an undeployed-function response. Firestore startup succeeded with Android Studio Java, and `FUNCTIONS_DISCOVERY_TIMEOUT=60` allowed discovery. The final tests passed; initial failures are not counted as successful runs.

## Android and feature evidence

Installed and launched the real application on `Pixel_8`, emulator serial `emulator-5554`, Android 15/API 35. The host Android SDK provides ADB/emulator tooling. ADB port forwarding resolved the emulator's failed host-network connection.

| Role | Runtime evidence and limits |
| --- | --- |
| Student | Launch/onboarding/login, populated Today/timetable/events, successful RSVP and QR ticket, menu/cart/order creation, route/stops with unavailable ETA, report submission, Activity, Discover, profile and logout. Registration component passed instrumentation. Every filter, profile edit, club follow, and notification setting has not been tested on-device. |
| Faculty | Local login/workspace, dark-theme capture and successful pasted-ticket attendance; Firestore confirmed attended, and replay was rejected. Full publication/edit UI coverage remains partial. |
| Admin | Successful local claim-based login/dashboard; app-created report visible and assigned through UI. Production bootstrap and invitation delivery unverified. |
| Vendor | Local assigned menu/order queue loaded; Android acceptance action verified as `ACCEPTED` in Firestore. Backend tests covered every valid order status and ownership denial; later vendor states were not all exercised on-device. |
| Driver | Local assigned bus/route, Android permission grant and successful trip start/end verified. Active-trip screen showed waiting for GPS. Real moving-device GPS and production location accuracy remain unverified. |

Emulator database verification confirmed `demo_event_demo_student`, a server-priced INR 15 order assigned to `demo_vendor`, and the app-created report in `ASSIGNED` status. The demo student also placed an INR 50 order against a retained integration-test canteen; this is local test data, not production activity. After the theme-recreation fix, a second ticket created by an authenticated emulator callable was checked in through the faculty UI after toggling night mode; both attendance cards updated to `ATTENDED` without restarting the app.

## Demo delivery

Twenty-one genuine PNGs are in `demo/screenshots/`, indexed by `demo/captures.json`. The 89.45-second ADB recording is in local `demo/Civora-Demo.mp4`; its MP4 duration and complete container were checked. Reproducible setup and demo credentials are in `demo/README.md`. Local APKs are `demo/apk/Civora-live-debug.apk` and `demo/apk/Civora-emulator-debug.apk`. Neither APK nor video is committed.

## Product quality review

Provisional engineering scores reflect code, emulator tests and the limited device review; they are not production benchmarks.

| Category | Score / 10 | Evidence |
| --- | --- | --- |
| UI design | 7 | Consistent Compose screens and genuine captures; generic launcher branding and weak light status-bar contrast remain |
| User experience | 7 | Daily overview, empty states and role routing work; error messages are often generic |
| Navigation | 7 | Main modules and role routing exercised; full back-stack coverage is incomplete |
| Feature completeness | 7 | Core workflows exist; QR camera scanning, uploads and background GPS are absent |
| Backend quality | 8 | Transactions, authoritative pricing, idempotency and integration tests; bootstrap spans two services |
| Security | 7 | Custom claims, ownership and deny-by-default rules tested; App Check and institutional admission are not enforced |
| Performance | 5 | Bounded queries; local Functions cold starts are slow; no device benchmark or production latency measurement |
| Stability | 7 | Login crash fixed and tested; limited Android-version/device coverage |
| Product usefulness | 8 | Connected campus workflows exercised with fictional data; no real-campus pilot evidence |
| Release readiness | 2 | Production services, billing, bootstrap and live verification outstanding |
| Overall | 6.5 | Arithmetic mean; local prototype/demo assessment |

Five principal remaining weaknesses:

1. Live Firebase services and approved billing are not ready, preventing a secure production deployment.
2. No confirmed production admin or real campus/role data; production workflows cannot be validated.
3. App Check/institution admission and operational abuse protections remain incomplete.
4. Real FCM delivery, moving-device GPS, Android-version compatibility and performance remain untested.
5. Full role UI, search/filter, profile/settings and invitation-delivery coverage remains incomplete; generic errors and minor visual polish remain.

## Necessary manual actions

1. Decide whether to enable Blaze for `civora-8f56e`; no upgrade is authorized by silence.
2. In Firebase Console, initialize Authentication and enable **Sign-in method → Email/Password**. Initialize Firestore in the intended region with secure rules; no database region was guessed.
3. After billing/setup is approved, deploy with `firebase deploy --project civora-8f56e --only 'firestore,functions'`, then verify functions/regions, index readiness and logs.
4. Register the intended administrator normally and explicitly confirm its UID. Supply trusted operator credentials and run the hardened bootstrap with `GCLOUD_PROJECT=civora-8f56e`; refresh the login token and verify admin routing.
5. Configure real campus data and validate live FCM/GPS and complete role UI workflows before a pilot.

## Git and final status

Source fixes, project mapping, documentation and actual screenshots are intended for a normal `origin/master` push. No force push, private Firebase configuration, service-account keys, dependencies, APKs or video are included. Commit/push result and final device outcomes are recorded in the final delivery message.

**Ready for a supervised local demo; not ready for a live pilot.** Automated backend checks and actual Android workflows support the local demo. Disabled live services and outstanding billing/admin setup prevent a production-ready claim.
