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

- 13 October verification: `gradlew.bat test assembleDebug lintDebug --max-workers=2` passed with exit code 0 in 2m 26s. The debug unit/build/lint checks are locally verified; this does not prove physical-device or production behavior.
- 13 October backend verification: `npm --prefix functions test` exited 0. 27 tests were discovered, 8 passed, 0 failed, and 19 were intentionally skipped because emulator-backed integration tests require the isolated Firebase emulator suite. No skipped test is reported as passed.
- Physical-device coverage remains BLOCKED: no authorized handset was connected; only the existing emulator was observed. See `docs/PHYSICAL_DEVICE_VALIDATION.md` for the non-destructive evidence and exact remaining manual gates.

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

**NOT READY FOR PRODUCTION.** Staff responsibility transfer/deactivation/deletion, academic effective-date overrides and daily upload authorization limits are implemented and locally tested. Android build/unit/lint checks and the non-emulator backend checks passed on 13 October 2026; emulator-backed backend cases remain skipped in this direct run. Physical-device journeys and live verification remain incomplete. Institutional authorization, operator contacts, retention/deletion activation, Firebase billing/regions/deployment, signing and publication remain separate approval gates. No automated production deletion is active. Review commit reference: `ab162e8` plus the commit created with this tracker update.
