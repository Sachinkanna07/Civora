# Security and deployment assumptions

## Trust boundaries

Public onboarding has no role argument. Android reads privileged roles from Firebase ID token claims; a users document is a profile, not authorization. Callable functions derive UID and role from verified authentication, never request data. Public profile creation permits only student, matching UID/email and server creation time. Profiles cannot edit role, UID, email or creation time. Faculty department cannot be edited through the client. Existing untrusted role values must not become claims during migration.

All event registration/cancellation, check-in, food orders/transitions, menu availability, report submissions/transitions, campus content/data, invitations and trip start/end run in callable functions. Rules deny all direct workflow writes, including administrators. The Admin SDK bypasses rules, so each function independently checks auth, ownership, size/type/range and transitions. Food price and vendor ownership are loaded from menu/canteen documents. Event registration uses a transaction on the event counter and deterministic per-user registration; missing counters fail closed. QR tokens are random, server stored, scoped to organizers, time-window constrained and single-use.

Do not deploy these rules until legacy profiles/registrations have been normalized using the trusted migration script, with registration writes paused and a database backup. Privileged claims must be assigned by a trusted operator after verifying staff identity; never copy an old public self-selected role into claims. Bootstrap the first administrator using the trusted script. Invitations cannot assign administrator or overwrite an existing administrator.

## Read/query matrix

All campus resources require sign-in. Campus announcements/events/schedules/menu/routes/clubs/places are campus-readable. Department/audience filters personalize discovery; they are **not confidentiality boundaries**. Do not publish confidential staff material into these collections.

Private orders: owner userId, assigned vendorId, or admin. Reports: createdBy or admin. Tickets/attendance: userId, assigned organizerId faculty, or admin. Activities/inbox: own userId only. Profile/devices/follows: own UID only (admin can read profiles). Private queries explicitly contain their ownership where filter; no unrestricted student/vendor collection reads. Trip location writes verify driver claim, trip assignment, current bus assignment and active state. Writes are throttled and captured positions older than 60 seconds are rejected to prevent queued offline data from looking fresh.

Only own notification read flag and own club follow records may change directly. GPS publishes every 15 seconds while the driver screen is resumed. No background tracking. Location permission denial pauses sharing; trips show stale/unavailable states.

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

Remaining real-world review: institutional identity/email-domain admission policy, campus boundary (this MVP is one campus per Firebase project), abusive signup/callable rate limiting, App Check enforcement, data retention, staff provisioning verification, invitation delivery and role revocation/token refresh behavior. App Check is intentionally not enforced in source until Android attestation is configured; deploy-time policy must be reviewed before public distribution.

Firestore rules are validated with the Firestore emulator. Callable workflow tests should also run against Auth/Firestore/Functions emulators; production console/auth/FCM and real GPS need device validation after configuration.

References: [custom claims](https://firebase.google.com/docs/auth/admin/custom-claims), [callable functions](https://firebase.google.com/docs/functions/callable), [Firestore transactions](https://firebase.google.com/docs/firestore/manage-data/transactions).
