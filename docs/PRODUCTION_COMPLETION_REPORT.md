# CIVORA — PRODUCTION COMPLETION REPORT

Evidence review: 12 October 2026, Asia/Kolkata. This report records the current review candidate; it does not certify production completion. See [the tracker](PRODUCTION_COMPLETION_TRACKER.md) for individual gates.

## A. Repository

Current base HEAD: fd0b068 on feat/civora-production-completion. Remote master independently verified/fetched: 0f09980. Existing user changes were preserved.

| Commit | Change |
|---|---|
| eb2a8cd | Campus workflows and production safeguards; preserved existing v2 work |
| f26ee72 | Protected access revocation and duplicate checkout contention recovery |
| c8758a0 | Verified KPRIET admission, approved exceptions, campus timezone and push isolation |
| d7db5a7 | Student inset consumption and guarded restored workspace navigation |
| 89701ce | Evidence, research, API/privacy/release materials and CI promotion gate |
| ddd741a | Current Android CI setup without retired SDK tools |

[Draft PR #1](https://github.com/Sachinkanna07/Civora/pull/1) is open against master. Nothing has been merged or deployed. Credentials, signing material, raw logs, emulator state and release binaries are excluded from Git.

## B. Product R&D

Desk research identified fragmented notices, ambiguous pickup status, meal-pass replay, stale transport positions, maintenance accountability and unclear operational role boundaries. Solutions include Today/Services/Inbox/Profile, server-owned order/meal transitions, private evidence and administrator review, explicit trip consent/freshness and server-provisioned roles. This is desk research, not a completed campus interview study. Details and primary sources are in [product research](PRODUCT_RESEARCH.md).

KPRIET's official references were reviewed for campus identity, programmes, transport and website privacy. Public transport information was not imported as current bus operations. Institutional website contacts do not authorize Civora support or data processing. See [institution configuration](INSTITUTION_CONFIGURATION.md).

## C. UI/UX

Material 3 supports light/dark/system themes, reusable cards/status pills/spacing, feature navigation, named operational selectors, explicit loading/empty/error states and account settings for staff. Public registration has no privileged role selector. Admission has a dedicated verification/approval screen.

Actual API 35 captures exposed repeated top insets in the student shell; the fix consumes the outer scaffold padding. Component tests verify saved-state cart restoration and Back behavior. Full large-font, TalkBack, contrast, optical scanner and device-level form coverage remain incomplete. No broad accessibility pass is claimed.

## D. Features

| Role | Implemented scope | Remaining verification / engineering |
|---|---|---|
| Student | Admission, Today, schedules/notices, event tickets, saved food cart/orders, meal passes, transport preferences, reports, inbox/profile, deletion request | End-to-end device journeys, offline/process recovery, production deletion activation |
| Faculty | Content/event publishing, editing/cancellation, own attendance, account settings | Actual publish/edit/scan journey; attachments and semester exceptions |
| Administrator | Operational counts, invitations, campus records, admission exceptions, report assignment/review | Complete administrative edit/audit journey and staff transfer/deletion |
| Vendor | Assigned kitchen queue, preparation/readiness/pickup, menu availability, pause/estimates | Actual fulfillment/pickup journey |
| Mess | Assigned meal windows, eligibility, expiring passes, single consumption and summaries | Student/operator device and scanner journey |
| Driver | Assigned buses, explicit foreground trip sharing/stop, delay/breakdown controls, stale state | Physical movement, background survival, battery and real route validation |
| Maintenance | Assigned work, progress/private photos and administrator review request | Actual image upload/assignment/review journey |

Backend tests exercise the relevant ownership, pricing, transitions, capacity/replay and simulated location checks. They do not replace human/device verification.

## E. Backend

Cloud Functions use Node 22, pinned Admin 14.5.0 and Functions 7.4.0. Shared callable guards enforce current identity/role/access state, membership, production App Check and per-user/function limits. Deterministic IDs, transactions and idempotent recovery protect orders and attendance. Schedule conflicts serialize through day locks; event cancellation closes registration/check-in. Student deletion uses a durable retryable worker with Auth removal last.

Admission requires both current Auth email verification and a matching verified token, exact kpriet.ac.in domain or an approved email-bound exception, and server-issued membership. Client-controlled email names never assign roles. Own pending onboarding is allowed; campus data/media/workflows remain protected. Production deletion is disabled until reviewed policy and explicit activation flags are enabled.

Firestore listeners use bounded ownership/assignment queries. Rules reject client workflow mutations and protect membership/access/role fields. Storage requires authenticated private immutable JPEGs, size/dimension/content validation and assigned evidence access. FCM routing/registration logic exists; emulator mode disables automatic push registration. Live delivery is unverified. [API contracts](BACKEND_APIS.md) and [schema](FIRESTORE_SCHEMA.md) document the interfaces.

## F. Security

Twenty-seven backend behaviors now have passing local emulator evidence, including real local verification codes, stale token rejection, exception revocation, membership/rule boundaries, scoped media, duplicate checkout contention, quota limits, suspension, staff responsibility transfer and durable deletion. Dependency audit reports zero vulnerabilities across 294 packages. Emulator bypasses do not authorize production access.

[Security audit](SECURITY_AUDIT.json) gives a local source/rules score of 4/5 with residual findings for upload/signup cost controls, staff lifecycle, live configuration and institutional authorization. [Security assumptions](SECURITY.md) describe the remaining limits. No independent security certification is claimed.

## G. Deployment

Local Auth/Firestore/Functions/Storage emulators run isolated demo projects. Actual Android testing uses a separate task-owned API 35 AVD; original user AVD data and unrelated applications were preserved.

A read-only production Firestore request returned HTTP 403 SERVICE_DISABLED; Functions listing failed. No deployment was attempted. Current billing/Auth state is not inferred from older results. No administrative website/hosting deployment was added without a product need. Monitoring and rollback procedures are prepared in [the runbook](RELEASE_RUNBOOK.md); live App Check, FCM, indexes and Crashlytics receipt remain unverified.

## H. Testing

| Check | Actual result |
|---|---|
| Android unit tests, c8758a0 | 27 passed, zero errors/failures |
| Android build/lint, c8758a0 | Debug/instrumentation and optimized release APK/AAB built; lintDebug/lintVitalRelease passed; debug lint zero errors, 45 warnings |
| Latest Android inset/navigation fix, d7db5a7 | Full build passed; 27 unit tests; debug/instrumentation/optimized APK/AAB; lint zero errors, 45 warnings |
| Combined local backend | 27 behaviors have passing evidence; the complete run was 26/27 before correcting an admin fixture, and the corrected two-test staff lifecycle suite passed |
| Device suite, current working tree | Four passed on API 35 in 47 seconds after replacing unstable system-Back injection with the visible app Back control |
| Bounded contention | Fifty orders at concurrency five, 40,671 ms including setup; eight duplicate requests return one stable order/token |
| Full npm audit | Zero vulnerabilities, 294 dependencies |
| Remote CI | Backend passed on the corrected run. Initial Android setup failed requesting retired SDK tools and was corrected; Android/device jobs pending, no all-green CI claim |

Raw logs are local ignored evidence. Earlier failed/resource-interrupted runs remain distinct from the final passing suite. [Performance report](PERFORMANCE_REPORT.md) states measurement conditions and limits.

## I. Release

Local binaries are under demo/final/apk and demo/final/aab, identified by [artifact hashes](../demo/final/artifact-hashes.json). The debug APK is development-signed and configured only for isolated emulator services. Optimized APK/AAB are unsigned inspection candidates. The latest layout/navigation build passed and replacement hashes identify d7db5a7, the last Android source change. Device rerun/captures are pending.

Package com.sachinkanna.civora, versionCode 2/versionName 2.0, min API 26/target API 36. Production signing requires all four secure environment values; no key has been created. R8/resource optimization and HTTPS-only release configuration are prepared. Crashlytics build IDs are generated; mapping upload is explicitly opt-in and has not run. [Play materials](PLAY_STORE_MATERIALS.md) and [privacy text](PRIVACY_POLICY_DRAFT.md) are drafts; public URLs, accountable publisher, Data Safety and foreground-service declaration remain required.

## J. Demo

Current actual captures include onboarding and student Today at c8758a0; food placement reached the isolated backend. A genuine initial food screen recording exists locally. These precede the inset fix and are not the final seven-role demo. Final screenshots/video are still being collected.

Runtime: Windows host, Android 15/API 35 Google Play x86_64 emulator, 720x1280 at 320 dpi; separate task-owned AVD and loopback Firebase emulators. Accounts and campus fixtures are explicitly fictional DEMO data. No production user data or supplied personal student email was used. Simulated GPS is not evidence of physical tracking.

## K. Product assessment

Provisional engineering judgments, not benchmark scores or completed user research:

| Area | /10 | Basis / limit |
|---|---|---|
| UI/UX | 7 | Coherent Material 3 components; real layout fix found; broader accessibility pending |
| Functionality | 7 | Seven role implementations and broad backend coverage; staff lifecycle/academic exceptions incomplete |
| Reliability | 6 | Transaction/idempotency tests pass; offline/process/live-device coverage incomplete |
| Security | 7 | Verification, roles, membership, private media and negative tests; residual lifecycle/cost/live gates |
| Performance | 5 | Small optimized candidate and bounded emulator correctness; physical/cloud measurements insufficient |
| Production readiness | 3 | Unsigned, undeployed, policies/authorization incomplete and device journeys pending |

## L. Blockers

Technical work remaining: approved orphan cleanup; remaining seven-role, scanner/photo, offline/reconnect, process recovery and accessibility journeys. Staff responsibility transfer/deletion, academic effective-date overrides and daily upload authorization limits are implemented and locally tested.

External approvals must identify an accountable institutional operator/publisher and authority to process campus data; confirm approved roles/exceptions and current operational records; supply monitored Civora support/privacy contacts and public app policy/deletion URLs; approve retention including triggers, backups and voluntary versus institutional deletion; authorize Firebase billing, budget ownership, regions and deployment/IAM; supply securely held signing configuration and approve attestation/diagnostic setup; authorize pilot and Play publication. Public website references do not grant any of these approvals.

Manual/live validation: real email delivery, release Play Integrity, private-service enforcement/indexes, FCM/routed taps, Crashlytics receipt, representative Android phones, optical QR/camera, moving GPS and foreground/background/battery behavior. Production deletion and all scheduled retention remain inactive.

## M. Final verdict

**NOT READY.** The app and backend have substantial executed local test evidence, and a draft PR/release preparation is reviewable. Final role/device evidence, engineering gaps, live deployment verification, signing and institutional policy/authorization prevent controlled-pilot or release-candidate readiness. Publishing is not the only remaining task.
