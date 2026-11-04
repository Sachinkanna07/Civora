# Civora product research and implementation decisions

Reviewed 10 October 2026. This is desk research plus source/runtime analysis; no campus interviews or usability study is claimed.

On 11 October, the user supplied KPRIET as the target. Its [website](https://www.kpriet.ac.in/), [programmes](https://www.kpriet.ac.in/study-at-kpriet), [transport](https://www.kpriet.ac.in/transport), [contacts](https://www.kpriet.ac.in/contact) and [website privacy notice](https://www.kpriet.ac.in/index.php/privacy-policy) were reviewed. The shared student/staff portal supports identity verification without treating email as role authority. Transport allotment remains institutional; app route/stop selection is a tracking preference. The website privacy notice is reference material, not an authorized Civora policy. Operational campus data and Civora contacts require authorization; no official endorsement, ERP integration or current live route import is claimed.

## Evidence and practical patterns

- [University of Cincinnati shuttle services](https://www.uc.edu/about/parking/transportation/shuttle-services.html) separates route schedules and live tracking. Civora should preserve route/stop information when GPS is unavailable, show the timestamp and distinguish a calculated estimate from a timetable.
- [Square KDS](https://squareup.com/us/en/point-of-sale/restaurants/kitchen-display-system) uses digital tickets and explicit fulfillment stages. Its [order completion guidance](https://api.squareup.com/help/us/en/article/8171-complete-orders-with-square-kds) distinguishes preparation and expediting. Civora uses one kitchen queue per authorized vendor, prominent pickup tokens and a pause control; it does not invent payment confirmation.
- [FMX work order capabilities](https://www.gofmx.com/wp-content/uploads/2019/12/Work-Order-Software-Datasheet.pdf) describes routing work to appropriate teams. Civora separates triage, worker progress, evidence and administrator approval rather than treating a report as a single status toggle.
- [Android navigation principles](https://developer.android.com/guide/navigation/principles) inform predictable system Back, saved navigation state and role boundaries. Nested food stages and event details need their own Back handling.
- [Android foreground location requirements](https://developer.android.com/about/versions/14/changes/fgs-types-required) require declared types and granted location permission before starting the service. Civora starts sharing only after an explicit driver action, shows a persistent notification and stops outside an active assigned trip.
- [Firebase callable functions](https://firebase.google.com/docs/functions/callable), [transactions](https://firebase.google.com/docs/firestore/manage-data/transactions) and [App Check enforcement](https://firebase.google.com/docs/app-check/cloud-functions) underpin authoritative prices/counters and trusted identity. App Check complements role authorization; it does not replace it.
- [Firebase data deletion guidance](https://firebase.google.com/docs/extensions/official/delete-user-data) shows why deleting Auth alone is insufficient. Civora needs durable cleanup of linked records, nested histories and Storage media.
- [Play target API policy](https://support.google.com/googleplay/android-developer/answer/11926878) currently requires API 36 for standard phone app submissions. [Account deletion policy](https://support.google.com/googleplay/android-developer/answer/13327111) also requires a usable external deletion route. Preparing a Markdown policy does not satisfy public URL requirements.

## Role walkthrough before design

| Role / pressure | User problem and industry weakness | Civora response | Difficulty / priority | Cost and privacy trade-off |
|---|---|---|---|---|
| Student between classes | Modules hide the next actionable item; live labels can conceal stale data | Today surfaces actual next/current class, active food token, registered event and chosen boarding stop; Services holds discovery | Medium / P0 | Bounded listeners; no fabricated timetable, ETA or payment |
| Faculty preparing an event | Technical IDs and date strings cause errors; tickets are replayable without server authority | Named selectors, date/time pickers, bundled camera scanner, transactional single-use attendance | Medium / P0 | Camera permission only on demand; tickets contain no sign-in credentials |
| Kitchen during lunch | Menu administration crowds out urgent orders; repeated taps can skip states | Workload-first queue, 48–56dp actions, stable tokens, explicit accepted/preparing/ready/pickup transitions and pause | Medium / P0 | Server writes per transition; completion is distinct from payment |
| Mess operator | Ordering systems do not enforce meal eligibility or one entry per period | Separate halls, entitlements, bounded periods, expiring purpose-bound passes, deterministic check-in keys and daily counts | High / P0 | Meal history private to student/operator/admin; no subscription billing |
| Driver safely stopped | Screen-bound tracking stops when locked; false freshness and precision mislead riders | Explicit foreground service, visible stop action, assignment validation, fresh fixes, throttling and reconnect backoff | High / P0 | GPS/battery cost only during trips; simulated fixes labeled; physical-device testing required |
| Administrator | UID forms create assignment mistakes; aggregate numbers lack operational meaning | Human-readable staff directory and selectors, open-queue metrics, trusted provisioning and protected campus data operations | High / P0 | Android admin retained; avoids second web stack and extra hosting cost |
| Maintenance worker | Report queues lack ownership, work notes or evidence; self-approval bypasses review | Assigned queue, progress history, private completion images, request review and administrator resolution | High / P0 | Authenticated media reads; restrict worker to assigned tasks |

## Priorities and limitations

P0: preserve role/privacy boundaries, backend transactions, consent and reliable core workflows. P1: efficient staff forms, process-safe carts, offline/retry clarity, deletion and release checks. P2: registration waitlists, semester exceptions, detailed institutional analytics and additional discovery saved items. A P2 item must either be implemented/tested or absent from the visible supported experience; it cannot be advertised as complete.

Route estimates need ordered stop/segment logic. Straight-line distance to a chosen stop is only a coarse proximity estimate and must not be marketed as traffic-aware arrival time. Campus timezone must be explicit before cross-timezone institutional use. Schedules need serialized conflict checks rather than a client-only overlap warning. Live FCM, Play Integrity and Crashlytics require external configuration and supported-device evidence.

Firestore and Storage are usage-priced. The callable identity/deletion/rate guards add reads and occasional writes but reduce stale-role access and abuse. Ten-instance callable caps limit scale; they are not a billing ceiling. Use emulator stress tests only; production load tests and Blaze changes require separate authorization.

Single-campus isolation remains one Firebase project per campus. Audience filters personalize public campus content; they do not make those collections confidential. Institution admission, retention exceptions, support contacts and legal text require campus-specific decisions.
