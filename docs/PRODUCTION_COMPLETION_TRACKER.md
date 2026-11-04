# Civora production completion tracker

Updated: 13 October 2026 (Asia/Kolkata). Evidence describes executed checks. Local tests do not establish live readiness.

Current base HEAD: ab162e8. Remote master independently verified/fetched: 0f09980. Branch: feat/civora-production-completion. This tracker update and the physical-device validation record are part of the 13 October 2026 review commit.

Statuses: NOT_STARTED, IN_PROGRESS, IMPLEMENTED, TESTED, VERIFIED, BLOCKED. TESTED covers bounded checks; VERIFIED requires the stated actual workflow and environment.

| Requirement | Code | Backend/security | Device | Remaining work |
|---|---|---|---|---|
| Authentication / seven roles | IMPLEMENTED | TESTED | IN_PROGRESS | Seven role journeys; real email delivery and staff authorization |
| KPRIET verified-email admission | IMPLEMENTED | TESTED | IN_PROGRESS | Pending admission screen; institution-approved exceptions |
| Today / academic scheduling | IMPLEMENTED | TESTED | IN_PROGRESS | Semester exceptions and campus-approved academic records |
| Faculty / announcements | IMPLEMENTED | TESTED | NOT_STARTED | Publishing/editing journey; live push and attachment coverage |
| Events / attendance | IMPLEMENTED | TESTED | NOT_STARTED | Organizer/student/camera journey |
| Food / kitchen | IMPLEMENTED | TESTED | IN_PROGRESS | Device fulfillment/pickup; pay at counter only |
| Mess | IMPLEMENTED | TESTED | NOT_STARTED | Operator/student QR journey |
| Transport | IMPLEMENTED | TESTED | NOT_STARTED | Physical GPS, background behavior and battery |
| Reports / maintenance | IMPLEMENTED | TESTED | NOT_STARTED | Photo, assignment, worker and review journeys |
| Admin / directory / clubs | IMPLEMENTED | TESTED | NOT_STARTED | Administrative edit/audit coverage |
| Notifications / offline | IMPLEMENTED | TESTED | NOT_STARTED | Real FCM, routed taps, reconnect/process recovery |
| Design / accessibility | IMPLEMENTED | TESTED | IN_PROGRESS | Insets fix rebuild; large fonts, TalkBack and contrast |
| Firestore / Storage | IMPLEMENTED | TESTED | IN_PROGRESS | Live rules/index/attestation |
| Student privacy / deletion | IMPLEMENTED | TESTED | NOT_STARTED | Confirmation journey; production activation requires approval |
| Staff deletion / retention | IMPLEMENTED | TESTED | NOT_STARTED | Institutional retention approval, public URLs and production activation |
| App Check / abuse / monitoring | IMPLEMENTED | TESTED | NOT_STARTED | Live attestation/crash receipt; cumulative upload/signup budgets |
| CI / release / signing | IMPLEMENTED | IN_PROGRESS | IN_PROGRESS | Rebuild latest layout fix, remote CI, secure upload key |
| Screenshots / video / performance | IN_PROGRESS | TESTED | IN_PROGRESS | Final role captures/video; physical measurements |
| Production deployment / pilot | BLOCKED | BLOCKED | BLOCKED | Explicit billing/deployment/region approval and actual campus setup |

## Executed evidence

- 13 October student workflow verification on laptop-only Pixel_8 (`emulator-5554`) against the preserved `demo-civora` suite: authenticated student Today dashboard loaded seeded timetable, transport, announcement and event data; timetable, announcements, events and an existing valid QR ticket opened; food cart was populated with DEMO Vegetable Meal, a pickup order was submitted and reached `Placed` / `Waiting for the kitchen to accept your order`; transport showed the fictional route with an explicit no-active-trip / unavailable-ETA state; reports opened and the empty state showed `No reports to show`. Fresh screenshots are under `demo/screenshots/student-*-phase1.png`. No Civora crash or backend error was observed in the exercised flows. Profile was previously captured in `demo/screenshots/student-profile-phase1.png`; account-settings editing and activity-history depth remain to be completed. One coordinate attempt was intercepted by the emulator's Google Lens overlay and was not counted as an app defect.
- Overnight continuation: student Inbox/My history showed real local `FOOD_ORDER` / `Placed`, `EVENT_REGISTERED` / `Registered`, and seeded notice entries; logout and sign-in restoration returned to the authenticated timetable. Report form and empty history were re-opened, but final issue submission was not claimed because intermittent `uiautomator` null-root / exit-137 failures prevented reliable completion. Unit tests passed; no database-resetting integration suite was run against the active demo.

- 13 October verification: with the installed JDK 21 selected explicitly, `gradlew.bat testDebugUnitTest assembleDebug lintDebug --max-workers=2` passed. The fresh debug APK was assembled; debug unit/build/lint checks are locally verified and do not prove emulator, physical-device or production behavior.
- 13 October local Firebase verification: the isolated suite started successfully with demo project `demo-civora` using Java 17: Auth `9100`, Firestore `8090`, Functions `5010`, Storage `9190`. A first Java 21 attempt failed because the Firestore emulator exited with Windows code `3221225786`; no production project or service was contacted.
- 13 October device gate: the only initially connected ADB target identified as physical model `RMX5120 - 16`, so it was disconnected and excluded. The available laptop AVD did not reach an online ADB state during this run. No valid laptop-emulator UI, role, screenshot, screen-recording or instrumentation evidence is claimed.
- 13 October backend verification: `npm --prefix functions test` exited 0. 27 tests were discovered, 8 passed, 0 failed, and 19 were intentionally skipped because emulator-backed integration tests require the isolated Firebase emulator suite. No skipped test is reported as passed.
- Physical-device coverage remains BLOCKED and unverified: no handset is authorized for this task. Physical-device, live Firebase, real push delivery and hardware-dependent tests remain unverified. See `docs/PHYSICAL_DEVICE_VALIDATION.md` for the exact remaining manual gates.

- 12 October Android clean build: 27 unit tests passed; debug APK, instrumentation APK, optimized unsigned release APK and AAB built. `lintDebug` and `lintVitalRelease` passed. The full command completed in 7m 48s.
- 12 October combined backend suite: 27 tests executed against local Auth/Firestore/Functions/Storage; 26 passed initially and one new staff-transfer fixture failed because it omitted the active admin directory record. After correcting only that fixture, both staff lifecycle tests passed. All 27 behaviors therefore have passing executed evidence, with the complete pre-fix output retained locally in `docs/evidence/backend-current.log`.
- Bounded stress: 50 orders, concurrency five, 40,671 ms including fixture setup; eight identical concurrent checkout requests return the same order/token. This is emulator correctness evidence, not production throughput.
- 12 October API 35 connected suite: four tests passed in 47 seconds. Registration has no privileged role input; food cart survives saved-state restoration and explicit app-bar Back navigation; isolated project and disabled push auto-init are asserted. Two earlier attempts exposed a reproducible focus issue in the test's use of system Back after state restoration; the test now activates the same visible app Back control used by users.
- Actual Civora launch succeeded on the task-owned Android 15/API 35 x86_64 emulator at 720x1280/320 dpi. Student Today and food order placement reached the local backend. Runtime capture exposed duplicate top insets; d7db5a7 corrects consumption and guards restored admission navigation.
- Full npm audit: zero vulnerabilities across 294 dependencies. Admin 14.5.0, Functions 7.4.0, patched gRPC 1.14.6. Log: npm-audit-final.json.
- Updated d7db5a7 artifact hashes are recorded in demo/final/artifact-hashes.json. Release manifest inspection confirms HTTPS-only configuration; package/version/target API and unsigned AAB were checked. No signing key was created and no Crashlytics mapping uploaded.
- Read-only production Firestore returned HTTP 403 SERVICE_DISABLED; Functions listing failed. Billing/Auth state is not inferred from older checks. No deployment attempted.
- Current captures use fictional, explicitly labeled DEMO fixtures. Earlier October 8 captures are historical. Physical GPS, optical scanning, FCM and live attestation remain unverified.

Raw logs and emulator exports under docs/evidence are ignored. CLI logs may contain private account metadata; do not commit them. User AVD data and unrelated applications were preserved.

## Exit gate

**NOT READY FOR PRODUCTION.** Engineering evidence for this run: debug APK, Android unit tests and lint passed; the isolated Firebase suite started; backend tests reported 27 discovered, 8 passed, 0 failed and 19 skipped. Engineering completion is therefore partial because emulator-backed backend cases, connected instrumentation, seven-role UI journeys, screenshots and recording were not verified on a laptop emulator. Physical-device, live Firebase, real push delivery and hardware-dependent tests remain unverified. Institutional authorization, operator contacts, retention/deletion activation, Firebase billing/regions/deployment, signing and publication remain separate approval gates. No automated production deletion is active.
