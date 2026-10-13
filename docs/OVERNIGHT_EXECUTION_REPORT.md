# Civora overnight execution report

Date: 13 October 2026 (Asia/Kolkata)
Branch: `feat/civora-production-completion`
Starting commit: `fe1bcf3`
Final commit: pending this execution

## Environment

- Pixel_8 / `emulator-5554` was online and boot-complete.
- The isolated `demo-civora` Firebase suite remained running on Auth `9099`, Firestore `8080`, Functions `5001`, and Storage `9199`.
- No physical device, production Firebase project, billing, or institutional account was used.
- Existing untracked `demo/evidence/` was preserved and not committed.

## Verified student results

PASS: Today, timetable, announcements, event details, existing QR ticket, food cart/order submission/order tracking, transport, reports screen and Profile.

PASS: Activity history showed locally generated `FOOD_ORDER` / `Placed`, `EVENT_REGISTERED` / `Registered`, and seeded notice activity.

PASS: Profile logout and sign-in restoration succeeded with the fictional student account; the app returned to the authenticated timetable/dashboard.

PARTIAL: Account settings were opened and visible controls were inspected, but notification/theme persistence was not fully exercised.

PARTIAL: Issue submission was not completed. The UI form and empty report-history state were verified, but repeated accessibility hierarchy calls intermittently failed with `uiautomator` exit status 137 before a disposable submission could be completed.

BLOCKED: Controlled offline recovery and full new-registration/attendance chain were not completed in this execution.

Fresh student evidence is under `demo/screenshots/student-*-phase1.png`, including `student-activity-phase1.png`.

## Role results

Faculty, vendor, mess, maintenance, driver and administrator end-to-end workflows were not re-run in this bounded execution. Existing repository evidence remains valid only for the workflows explicitly stated in prior reports; no new role pass is claimed here.

## Tests

- `gradlew.bat testDebugUnitTest --no-daemon --max-workers=2`: PASS, Gradle build successful.
- Database-resetting backend integration tests were not run against the active demo session.
- Connected instrumentation was not re-run in this execution; the previously recorded laptop result remains 4 passed, 1 skipped, 0 failed.

## Defects and limitations

- No Civora crash or Firebase error was observed in the exercised student flows.
- The repository capture helper intermittently received `UiTestAutomationBridge` null-root / exit-137 failures; this limited repeatable automation of the report form and account-setting controls.
- No source defect was safely isolated and fixed during this execution.

## Production blockers

Complete role workflows, issue lifecycle resolution, controlled offline recovery, live FCM/App Check, physical hardware behavior, signed release configuration, institutional approvals, retention/deletion approvals, and live deployment verification remain open.

## Recommended next task

Run a fresh isolated emulator lifecycle on alternate ports for the complete backend suite, then perform faculty/vendor/mess/maintenance/admin/driver workflows with a stable UI automation session.
