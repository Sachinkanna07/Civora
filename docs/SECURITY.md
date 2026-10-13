# Security and deployment assumptions

## Production completion audit — 11 October 2026

Current additions supersede baseline statements about screen-bound GPS and absent uploads. A visible driver-initiated foreground location service, private report/evidence Storage rules, camera QR scanner, maintenance and mess claim roles now exist in the preserved v2 source. Validation results belong in `PRODUCTION_COMPLETION_TRACKER.md` and the completion report; this section does not itself certify them.

All callables pass a shared guard that checks current Auth identity/disabled state, compares current server claims with the token, rejects revoked sessions, checks the server-only profile access flag, blocks deletion-in-progress and applies exact per-user/function/window quotas. Production callables require App Check; emulators bypass attestation. Role authorization remains independently enforced inside each workflow. Institution authorization and direct Firestore/Storage App Check project enforcement remain external configuration gates.

Privileged direct reads require both the token claim and matching protected profile role, so a server-side role change invalidates old privileged tokens for those reads. The server-only `accessDisabled` profile field blocks direct reads/uploads without waiting for token expiry. Student deletion sets this flag in the same transaction as its receipt; missing profiles fail closed for Storage. Storage reuses its profile lookup and one report lookup to respect the [two-document cross-service limit](https://firebase.google.com/docs/rules/rules-behavior). Trusted operators use `functions/scripts/set-account-access.js UID disable|enable` with explicit project and ADC. Console-only Auth disable/revocation may leave direct cached tokens valid until expiry; update the protected flag first through the tool. Never let clients write access flags or role mirrors.

Student deletion uses recent password confirmation and a durable retrying server job. Rules block users with a deletion receipt; transactional student order/registration/report/pass operations recheck deletion before committing. Auth is removed after cleanup and registration capacity adjustments. Staff self-service deletion remains blocked. The administrator lifecycle workflow previews supported responsibilities, requires a named active replacement of the same role and explicit confirmation, transfers owned work, disables the protected profile, stops active driver trips, disables Auth, revokes sessions and records an audit. Tests cover faculty, vendor, driver, mess and maintenance responsibilities. Production execution still requires an approved operator and data-governance process.

Audit review checks create/update parity, exact fields, ownership, claim authority, file type/size, staff assignment and data privacy. Image writes require an expiring server-issued permit after role/ownership checks and share a cumulative limit of 20 new paths per account/day; retries for the same path are idempotent. Residual risks include unactivated orphan-upload cleanup/retention, missing institutional authorization, partial operational audit coverage and live attestation/FCM/GPS validation. Exact callable quotas are per account and action. Notification fan-out stops at 5,000 recipients and records an operations-audit event when capped. These controls do not replace project-wide Firebase budgets or Authentication abuse controls.

The initial dependency audit reported nine moderate production-chain findings. After reviewing the official Node Admin migration notes and Node 22 compatibility, dependencies were pinned to `firebase-admin@14.5.0` and `firebase-functions@7.4.0`. A full audit exposed a dev-only gRPC certificate-validation advisory; the dependency override now pins `@grpc/grpc-js@1.14.6`. The executed full audit reports zero vulnerabilities across 294 dependencies. This is dependency evidence, not proof of application security.

## Trust boundaries

The KPRIET admission layer additionally requires current Auth email verification, a matching verified token, server-only membership grant/email and exact institutional domain or a reviewed email exception. Role prefixes are never inferred from email. Own onboarding profile access precedes admission; campus data, media and mutations do not. Activation and self-deletion are separate pending-identity endpoints, and production deletion is explicitly gated by reviewed retention/activation settings. See `INSTITUTION_CONFIGURATION.md` for exception approval, migration and remaining authorization boundaries.

Public onboarding has no role argument. Android reads privileged roles from Firebase ID token claims; a users document is a profile, not authorization. Callable functions derive UID and role from verified authentication, never request data. Public profile creation permits only student, matching UID/email and server creation time. Profiles cannot edit role, UID, email or creation time. Faculty department cannot be edited through the client. Existing untrusted role values must not become claims during migration.

All event registration/cancellation, check-in, food orders/transitions, menu availability, report submissions/transitions, campus content/data, invitations and trip start/end run in callable functions. Rules deny all direct workflow writes, including administrators. The Admin SDK bypasses rules, so each function independently checks auth, ownership, size/type/range and transitions. Food price and vendor ownership are loaded from menu/canteen documents. Event registration uses a transaction on the event counter and deterministic per-user registration; missing counters fail closed. QR tokens are random, server stored, scoped to organizers, time-window constrained and single-use.

Do not deploy these rules until legacy profiles/registrations have been normalized using the trusted migration script, with registration writes paused and a database backup. Privileged claims must be assigned by a trusted operator after verifying staff identity; never copy an old public self-selected role into claims. Bootstrap the first administrator using the trusted script. Invitations cannot assign administrator or overwrite an existing administrator.

## Read/query matrix

All campus resources require sign-in. Campus announcements/events/schedules/menu/routes/clubs/places are campus-readable. Department/audience filters personalize discovery; they are **not confidentiality boundaries**. Do not publish confidential staff material into these collections.

Private orders: owner userId, assigned vendorId, or admin. Reports: createdBy, assigned maintenance worker, or admin. Tickets/attendance: userId, assigned organizerId faculty, or admin. Activities/inbox: own userId only. Profile/devices/follows: own UID only (admin can read profiles). Private queries explicitly contain their ownership where filter; no unrestricted student/vendor collection reads. Trip location writes verify driver claim, trip assignment, current bus assignment and active state. Writes are throttled and captured positions older than 60 seconds are rejected to prevent queued offline data from looking fresh.

Only own notification read flag and own club follow records may change directly. Driver-initiated GPS sharing runs in a location foreground service with a visible notification and stop action while a trip is active. Location permission denial pauses sharing; trips show stale/unavailable states. Real background execution, battery behavior and moving-device location remain unverified.

## Prototype rules and attack review

Rules require review before broad rollout. Emulator tests cover:
- public/other-user profile reads and privileged-role/extra-field writes;
- create-valid/update-invalid name size and role changes;
- forged workflow documents, counter writes and direct status changes;
- assigned versus unrelated vendor reads and private list query scope;
- immutable notification payload and own read-state updates;
- club ownership, parent existence and immutable follows;
- unrelated driver assignment, out-of-range GPS and location write throttling;
- faculty ticket ownership and denied direct attendance mutation.

Remaining real-world review: institutional authorization and role provisioning, campus boundary (one campus per Firebase project), Authentication signup abuse/budget controls, orphan-media cleanup, direct-service App Check enforcement, approved retention/deletion activation and invitation delivery. Exact-domain verified-email admission, email-bound exceptions and server-issued upload budgets are implemented and tested locally. Production callables enforce App Check in source. Register release signing fingerprints and verify attestation before deployment; the emulator bypass is limited to Functions emulator execution.

Firestore rules are validated with the Firestore emulator. Callable workflow tests should also run against Auth/Firestore/Functions emulators; production console/auth/FCM and real GPS need device validation after configuration.

References: [custom claims](https://firebase.google.com/docs/auth/admin/custom-claims), [callable functions](https://firebase.google.com/docs/functions/callable), [Firestore transactions](https://firebase.google.com/docs/firestore/manage-data/transactions).
