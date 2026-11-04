# Civora Play Store materials — working draft

App name: Civora

Short description: Everything on campus. One app.

Full description draft:

Civora connects your campus day with classes, notices, events, food, transport and campus support. View the timetable published by your institution, register for campus events and show your digital ticket, order campus food for pickup and follow kitchen progress, choose your boarding stop, and report campus issues with photos and status updates.

The configured campus is KPRIET. Access requires a verified email address and server-approved campus membership. An institutional email address does not determine a staff or student role. Civora is independently developed; institutional endorsement has not been confirmed.

Authorized staff workspaces support faculty publishing and attendance, kitchen fulfillment, mess meal verification, driver-initiated trip sharing, maintenance assignments and campus administration. Availability depends on your institution's configured services and your assigned role. Food orders use pay-at-counter. Bus arrival estimates are approximate and depend on fresh driver GPS and published route stops.

Optional camera access supports scanning/capture. Drivers initiate location sharing for active trips with a visible notification and stop control. Push notifications need device permission. Crash diagnostics are optional.

Do not submit this listing until supported workflows and live services pass the release gate. Remove claims for any module excluded from the final verified release.

## Required store assets and declarations

- Feature graphic specification: 1024×500 PNG/JPEG, no transparency; Civora mark and “Everything on campus. One app.”; use licensed original visuals. No feature graphic is claimed created here.
- Phone screenshots: genuine runtime captures under `demo/final/screenshots/`. Clearly disclose fictional demo data in internal review; do not misrepresent simulated GPS as campus operations.
- Privacy policy: operator-approved publicly reachable HTTPS app policy; `PRIVACY_POLICY_DRAFT.md` is only a draft. The institutional website notice is reference material, not Civora's policy.
- Account deletion: local in-app student deletion is tested; production deletion remains disabled pending approved retention/activation. Supply a usable external deletion request URL and complete staff transfer/deletion before release.
- Support: supply monitored Civora-controlled support/privacy contacts. Configurable values remain unset; institutional mailboxes must not be used without authorization.
- Data Safety draft: account identity, academic profile, order/registration/attendance/meal records, campus reports/photos, push token, driver precise/coarse location during trips and opt-in crash diagnostics. Decide collection/sharing/optional/required declarations from the actual deployed SDKs and institutional use.
- Content rating: complete the questionnaire based on the actual app. No rating or Play approval is claimed.
- Foreground service declaration: user-initiated location sharing for authorized campus drivers; provide truthful runtime video evidence and permission justification.

Release notes draft: Refined campus navigation and role workspaces; kitchen order queue and meal check-in; maintenance progress with private photos; camera event tickets; explicit foreground trip sharing; event lifecycle and schedule conflict controls; student account deletion and release/security preparation.

Signing, live backend verification, physical-device testing, public URLs and institution review remain required. This document is not a Play-ready submission.
