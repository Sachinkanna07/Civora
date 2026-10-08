# Civora 2.0 — baseline product and UX audit

Baseline: clean `master`, `0f09980`, reviewed on 8 October 2026. Android source/resources, Firebase configuration/workflows/rules, models/repositories/ViewModels/navigation, documentation and existing tests were inspected before implementation. All 21 genuine captures were indexed and reviewed; the 89-second recording was sampled at 10, 40 and 75 seconds. Evidence files are in `demo/screenshots/` and `demo/Civora-Demo.mp4`.

## Critical / pilot blockers

- Production billing remains disabled. Authentication returns `CONFIGURATION_NOT_FOUND`; Firestore and Functions APIs return `SERVICE_DISABLED`. No production setup, billing, privileged account promotion or data seeding is authorized by this transformation request.
- Maintenance workers and mess operators have no role, workspace, entitlement/assignment model or protected workflow. These are missing journeys, not visual defects.
- Driver tracking is tied to `ON_RESUME`/`ON_PAUSE`; locking the phone or switching apps stops sharing despite an active trip. A visible, explicitly started foreground location service is needed.
- Report photos require a pasted HTTPS URL. No capture/gallery/upload exists and no Storage rules are configured. New report evidence must remain authenticated and private.

## High severity task friction

- Food combines canteen chips, menu, cart and history in one long list. The recording shows an old success message while a new cart is being built. Menu selection, checkout and tracking need distinct screens and a persistent cart identity.
- Kitchen workers scroll past menu availability controls to reach orders. No incoming alert, pause/open controls or workload-first queue exists. Twenty-order tests are needed.
- Today calculates ETA to the first route stop; the profile has no selected boarding stop. The student's actual stop must drive the estimate, with freshness shown explicitly.
- Faculty editors require `yyyy-MM-dd HH:mm`, optional club IDs and image URLs. Date/time pickers, named selections, preview and upload are needed.
- Admin campus configuration requires reference IDs, raw UID assignments, paise and minutes-from-midnight. Routine operations need named pickers and normal price/time units.
- Report assignment accepts an arbitrary UID/team string. There is no trusted staff directory, worker workload or progress/evidence history.
- Attendance requires external scanning/pasted token strings; cards display registration IDs. A bundled camera scanner must still submit to the existing secure check-in callable.

## Medium severity consistency / reliability

- Today/Discover/Action duplicate entry points. The Action tab is ambiguous; a Services directory with Today, Inbox and Profile is clearer for this campus task set. This is a design inference from inspected flows, not a claim of a conducted user study.
- Hardware Back is intercepted at shell level and can leave detail pages before their internal back behavior. Major-tab state and detail back handling need consistent ownership.
- Long-lived success/error banners shift every screen and survive unrelated navigation. Use concise snackbars and contextual validation.
- Skeleton/empty/error/stale states are inconsistent. Several feed progress bars can stack; blank data and failure should remain distinguishable.
- Light screenshots show white status icons on a pale background. Page headers are oversized/duplicated; spacing and role surfaces need a restrained Material 3 system.
- Cart state is memory-only and screens hold independent request IDs. Process recreation and ambiguous network failure can undermine retry identity.
- All students subscribe to every feed immediately. Keep bounded listeners, scope sensitive queries and avoid unnecessary staff-only subscriptions.
- Theme is system-only; profile is a developer form with a photo URL. Academic/transport choices and preferences need simple sections.
- Notification taps reopen the app without a relevant destination. Inbox/history separation and safe entity routing need completion.
- Android backup templates permit broad transfer; authentication/cache/preferences need an explicit privacy policy.

## Low severity / lint and documentation

- Android launcher assets are the default Android robot. Replace with a recognizable Civora vector mark.
- Baseline lint: 0 errors, 43 warnings. Meaningful cleanup includes unused colors, modifier ordering, redundant manifest label and version-catalog consistency. Version/SDK notices will be evaluated without blind upgrades or suppression.
- Several no-op callbacks are preview/test-only; they are not dead production controls. The backup TODO is real template debt.
- README incorrectly says verified screenshots are absent and still describes screen-bound GPS/pasted scanning. Documentation must match the delivered v2 behavior.

## Preserved foundations and implementation decisions

Retain single-campus architecture, custom-claim authorization, deterministic order/registration keys, transactional capacity/pricing/state transitions, bounded Firestore queries and explicit production gates. Introduce maintenance and mess roles only through trusted provisioning. Preserve legacy record fields and document additive migration needs. Use Today / Services / Inbox / Profile, dedicated food stages and kitchen queues, private Storage attachments, camera attendance, named staff assignment and foreground trip tracking. Operational times, metrics and freshness must come from actual records.

## Baseline commands

- `gradlew.bat test assembleDebug lintDebug`: exit 0, 14 unit tests, APK built, 0 lint errors / 43 warnings.
- `npm --prefix functions test`: exit 0, 3 policy tests passed, 8 emulator-dependent tests skipped.
- ADB: `emulator-5554` available, Pixel_8 Android 15.
- Remote Firebase checks: authenticated access available; live service/billing failures listed above.

Acceptance is based on new backend tests, actual role workflows and v2 runtime captures. Compilation alone will not establish completion.
