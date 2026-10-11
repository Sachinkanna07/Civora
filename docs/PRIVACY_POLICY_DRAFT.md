# Civora privacy policy — institution review draft

Prepared 11 October 2026. This is an unpublished review draft. Target campus: KPR Institute of Engineering and Technology (KPRIET), Arasur, Coimbatore, Tamil Nadu, India, 641407. Civora is independently developed; endorsement/data-processing authorization is unconfirmed. The accountable Civora operator, authorized Civora-controlled contacts, public policy URL and external deletion URL remain required. Institutional mailboxes are reference contacts, not automatically Civora contacts.

Civora connects campus schedules, notices, events, meals, transport and maintenance. Firebase hosts account and campus data. Protected access requires verified email, server-issued membership, server-assigned roles and operational assignments. Exact `kpriet.ac.in` addresses may activate membership; institution-approved exceptions are email-bound. The domain never determines a staff role. One Firebase project represents one campus.

## Information handled

| Category | Purpose / access |
|---|---|
| Name, email, authentication identifiers | Sign-in, profile and institution-authorized staff provisioning. Password handling uses Firebase Authentication; application documents do not store passwords. |
| Department, year, section, preferred route/stop | Personalized timetable, announcements and transport. Academic/profile fields remain within authenticated campus operations. |
| Event registrations, attendance and opaque QR passes | Capacity control, tickets and organizer attendance. Student, assigned organizer and administrator access only. |
| Food order items, pickup tokens and status | Kitchen fulfillment. Student, assigned vendor and administrator access. Payment is represented as pay-at-counter; Civora does not process cards or claim online settlement. |
| Mess entitlement, expiring passes and meal check-ins | Meal eligibility and duplicate entry prevention. Private records are scoped to their student/operator/admin. |
| Campus reports, location text, progress and photos | Issue resolution by authorized operations staff and assigned maintenance worker. Report photos use authenticated Storage access. |
| Driver coordinates, accuracy and timestamps | Bus location during explicitly initiated active trips. Visible foreground notification provides stop sharing. No outside-trip or stealth tracking is intended. Student location is not collected for viewing routes. |
| Device push tokens and inbox read state | Optional campus notifications, with user preference and Android permission checks. Device unregistration on logout is best effort if connectivity is unavailable. |
| Optional crash diagnostics | Firebase Crashlytics collection defaults off. The user may opt in; logout disables collection. No Analytics SDK or personalized tracking is added. Live delivery remains unverified. |

Camera permission is requested for ticket scanning/capture. Gallery selection uses Android's picker. Photo processing resizes and JPEG-encodes images, so original EXIF metadata is not intentionally carried into uploads. Do not submit unrelated sensitive documents or photos of people without an appropriate campus basis.

## Deletion and retention

Proposed retention is GPS 7 days, food orders 90 days, attendance 1 calendar year, campus reports 1 calendar year after resolution, and operational logs 90 days. These proposals are not approved or activated. Production deletion is gated; emulator tests do not authorize production deletion. See [configuration and approval boundaries](INSTITUTION_CONFIGURATION.md). Voluntary account deletion, institutional records, unresolved reports, backups and staff handover need review before activation.

Student account deletion requires password confirmation and a server-side request. Active food orders must first be completed or cancelled. A durable worker disables the account and removes the profile/devices, private reports and associated images, registrations, attendance, order/meal history, follows, activities, notifications and meal passes. Event capacity is adjusted transactionally. Authentication is removed last, and transient failures are retried. A restricted deletion receipt retains UID, status and timestamps so operators can confirm completion. Staff must have responsibilities transferred by campus operations before deletion; a complete staff transfer/deletion tool is still required before public release.

No institution-wide automatic retention schedule is activated yet. Proposed operational review: expire unpublished/orphaned uploads; set short retention for inactive trip coordinates and transport disruptions; retain institutional attendance only for an agreed academic purpose; remove operational logs according to a defined support window. The institution must approve actual durations, lawful retention exceptions and any backup retention. Source preparation does not establish those controls are deployed.

Existing copies in institution backups, exported data or previously shared material require a documented operator procedure. Deleting a production account must include reconciliation with those systems. Do not promise instantaneous removal from backups.

## Release prerequisites

Publish final institution-approved text on a publicly accessible HTTPS URL. Provide an external deletion request route that can be used without reinstalling the app. Complete Google's Data Safety declaration based on actual activated Firebase services and SDK collection. Test foreground location, Crashlytics consent, account deletion and FCM against the real configured project. The draft does not claim compliance approval.
