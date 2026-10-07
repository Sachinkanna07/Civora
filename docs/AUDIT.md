# Repository audit ? 7 October 2026
Baseline: master, 2b74de9; remote master already current. Local gradle.properties modifications preserved.

Working: branded launch/onboarding, email authentication, announcement/event UI and filters, Firebase-backed lists, basic unit tests.

Findings: public privileged role selection and role trusted from profile; no rules/indexes/backend; unbounded collection fetches and detail-via-feed reads; RSVP without capacity enforcement; My department matches any department; no expiry/audience filtering; no functional food/transport/reports/timetable/community; activity and role dashboards are placeholders; nonfunctional search and quick actions; duplicated dashboard and compressed feature files; detail errors can spin forever; raw authentication exceptions; portfolio language; Firebase configuration missing handling. Existing unit tests cover only filter smoke checks.

Direction: retain branding/auth/event and announcement concepts, trust server custom claims, use bounded realtime queries, server-authoritative transactional workflows for sensitive actions, separate feature screens, typed campus models, observable immutable state, explicit empty/error/retry, Firestore rules/indexes, emulator security tests and domain tests. No payments or LMS.

## Completion audit

The working refactor now includes the student shell and all requested MVP workflows, separate staff workspaces, Firestore rules/indexes and server functions. The complete brief was received on 7 October; earlier attachments stopped partway through events. Existing edits were preserved and audited as the baseline rather than discarded.

Follow-up corrections: removed the unused role argument from auth forms and obsolete role-selection route; merged overlapping timetable periods before computing gaps; corrected admin announcement/timetable queries to include campus-wide records; filled optional admin-form values and displayed timetable reference IDs; allowed mock GPS only in debug; prevented cross-account push display; protected workspace actions on session changes; rejected negative event counters; added a directions fallback; removed development-phase wording; maintained contrast for dark branded onboarding in either system theme.

The Firebase rules auditor review is recorded in `RULES_AUDIT.json`. Department metadata personalizes campus-readable content and is not an institutional admission check. Sensitive workflows are enforced in callable functions as well as denied for direct client writes.

Intentional boundaries: foreground-only location, approximate ETA, pasted/scanned attendance payload validation, HTTPS image references, external map directions, bounded recent feeds and one campus per project. Production Firebase configuration, App Check policy, real campus data and physical-device GPS/push validation remain external. See README for setup and testing commands. The pre-existing local `gradle.properties` edits remain uncommitted.
