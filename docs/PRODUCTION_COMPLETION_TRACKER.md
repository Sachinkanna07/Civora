# Civora production completion tracker

Updated: 11 October 2026 (Asia/Kolkata). Evidence describes executed checks. Local tests do not establish live readiness.

Starting HEAD: c18da64. Remote master independently verified/fetched: 0f09980. Branch: feat/civora-production-completion. Pre-existing v2 work was preserved in eb2a8cd. Source checkpoints: eb2a8cd, f26ee72, c8758a0, d7db5a7.

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
| Staff deletion / retention | IN_PROGRESS | IN_PROGRESS | NOT_STARTED | Responsibility transfer/cleanup; retention approval and public URLs |
| App Check / abuse / monitoring | IMPLEMENTED | TESTED | NOT_STARTED | Live attestation/crash receipt; cumulative upload/signup budgets |
| CI / release / signing | IMPLEMENTED | IN_PROGRESS | IN_PROGRESS | Rebuild latest layout fix, remote CI, secure upload key |
| Screenshots / video / performance | IN_PROGRESS | TESTED | IN_PROGRESS | Final role captures/video; physical measurements |
| Production deployment / pilot | BLOCKED | BLOCKED | BLOCKED | Explicit billing/deployment/region approval and actual campus setup |

## Executed evidence

- c8758a0 Android build: 27 unit tests passed; debug APK, instrumentation APK, optimized release APK and AAB built. lintDebug and lintVitalRelease passed; debug lint reports zero errors and 45 warnings. Log: campus-admission-android-final.log. d7db5a7 requires a new build/device verification.
- Latest combined backend suite: 25 tests passed, zero failed/cancelled/skipped, 599,343 ms. Log: campus-admission-backend.log. Actual local Auth OOB verification/activation, exact-domain admission, exception email binding/revocation, protected membership/rules/media, ownership, workflow transitions, conflict races and deletion guards were exercised.
- Bounded stress: 50 orders, concurrency five, 40,671 ms including fixture setup; eight identical concurrent checkout requests return the same order/token. This is emulator correctness evidence, not production throughput.
- API 35 device suite at c8758a0: four tests passed in 23.448 seconds on retry. Registration has no privileged role input; food cart survives saved-state restoration and Back navigation; isolated project and disabled push auto-init asserted. Initial attempt failed with a process-start ANR before tests; retained as failed evidence.
- Actual Civora launch succeeded on the task-owned Android 15/API 35 x86_64 emulator at 720x1280/320 dpi. Student Today and food order placement reached the local backend. Runtime capture exposed duplicate top insets; d7db5a7 corrects consumption and guards restored admission navigation.
- Full npm audit: zero vulnerabilities across 294 dependencies. Admin 14.5.0, Functions 7.4.0, patched gRPC 1.14.6. Log: npm-audit-final.json.
- c8758a0 artifact hashes are recorded in demo/final/artifact-hashes.json; release binaries are unsigned. They will be replaced after the latest source build. No signing key was created and no Crashlytics mapping uploaded.
- Read-only production Firestore returned HTTP 403 SERVICE_DISABLED; Functions listing failed. Billing/Auth state is not inferred from older checks. No deployment attempted.
- Current captures use fictional, explicitly labeled DEMO fixtures. Earlier October 8 captures are historical. Physical GPS, optical scanning, FCM and live attestation remain unverified.

Raw logs and emulator exports under docs/evidence are ignored. CLI logs may contain private account metadata; do not commit them. User AVD data and unrelated applications were preserved.

## Exit gate

**NOT READY.** Engineering gaps include staff responsibility transfer/deletion, semester exceptions and upload/signup cost controls. Device journeys and live verification are incomplete. Exact-domain verification is implemented, but institutional authorization, operator contacts, retention/deletion activation, Firebase billing/regions/deployment, signing and publication remain separate approval gates. No automated production deletion is active.
