# KPRIET configuration and approval boundaries

Reviewed 11 October 2026. Civora is independently developed; institutional endorsement or authorization has not been confirmed. Public institutional reference material does not authorize operational integration or data processing.

The user supplied KPR Institute of Engineering and Technology, Arasur, Coimbatore, Tamil Nadu, India, 641407, exact email domain `kpriet.ac.in`, and timezone `Asia/Kolkata`. The [official website](https://www.kpriet.ac.in/), [contact page](https://www.kpriet.ac.in/contact), [programme catalogue](https://www.kpriet.ac.in/study-at-kpriet) and [transport page](https://www.kpriet.ac.in/transport) were reviewed. Its student/staff portal is shared; an email domain cannot identify a role.

## Admission

`functions/campus-config.json` is the source for backend policy and generated Android campus constants. Firebase email verification is required. `activateCampusAccess` checks both current Auth verification and the ID token, exact domain or a reviewed email exception, current role, account suspension and deletion state. It writes immutable client-side membership fields; clients cannot grant membership or exceptions. Domain prefixes and email names never assign privileged roles.

Unverified/pending users can create/read only their own onboarding profile, send verification mail, refresh their token, log out or request account deletion where enabled. Campus feeds, media, devices and operational mutations remain blocked. Password-reset links for invited staff do not substitute for email verification.

Verified campus administrators can find an account by email in **Admission**, approve/revoke an exception with an institutional approval reference, and provision roles separately through **Invite**. Exceptions are bound to the exact current email. Approval does not automatically verify email or assign a role; revocation clears the membership grant. The trusted ADC alternative is `functions/scripts/set-admission-exception.js UID approve|revoke APPROVAL_REFERENCE` with the explicitly approved project. Do not run against production without institutional authorization.

Existing accounts must complete verification/activation or receive a reviewed exception during migration. Never mark production emails verified merely to bypass admission. Emulator fixtures explicitly model verified accounts and reviewed exceptions; they are fictional and do not prove real email delivery or institutional approval.

## Time and transport

All timetable calculations, holiday dates, event editors and displayed timestamps use `Asia/Kolkata`, independent of device timezone. Persisted event times remain UTC timestamps. Timetable semester exceptions still need completion and campus-approved academic data.

The transport website describes office-controlled route allocation and boarding changes. Civora's preferred route/stop is a tracking subscription, not a transport allotment, entitlement or payment receipt. Public route descriptions and approximate published schedules have not been imported as verified live operational data. Actual buses, drivers, ordered stops/coordinates and allocation rules require approval and current operational verification.

## Contacts and privacy

The [institutional privacy notice](https://www.kpriet.ac.in/index.php/privacy-policy) lists an institutional privacy mailbox and describes its websites; it is not Civora's policy. Civora-controlled `supportEmail` and `privacyEmail` remain empty and configurable until authorized values are supplied. The supplied personal student address is not stored in source, fixtures or release materials.

Civora still needs an accountable operator/publisher, approved support/privacy contacts, an app-specific public HTTPS policy, external deletion route and institutional data-processing authorization. Do not use institutional mailboxes as Civora support without authorization.

## Proposed retention — not activated

| Data | Proposed period | Trigger |
|---|---|---|
| GPS | 7 days | After capture; confirm last-fix versus history scope |
| Food orders | 90 days | Confirm fulfillment/cancellation reference point |
| Event attendance | 1 calendar year | After event/attendance; confirm institutional record requirements |
| Campus reports | 1 calendar year | After resolution; unresolved cases require separate policy |
| Operational logs | 90 days | After log creation; include audit and backup decisions |

Configuration has `retention.approved=false` and `productionDeletionEnabled=false`. No scheduled production deletion is activated. Production account-deletion requests/workers also remain gated until both settings are reviewed and explicitly enabled. Emulator deletion tests remain enabled in isolated demo projects. Review orphan media, quota TTL, backups, voluntary deletion versus institutional records and staff transfer/deletion before activation. Do not manually create production deletion receipts while the gate is closed.

Remaining approvals are institutional authorization/controller ownership, exceptions/staff identity, Civora contacts and policy/deletion URLs, retention/deletion, Firebase billing/regions/deployment/budgets, signing/attestation, live-device testing and publication. Source preparation grants none of these approvals.
