# Civora callable API contract

All callables are exported from `functions/index.js` in `us-central1`. Android uses the Firebase Functions SDK. Requests are objects; callable errors use standard Firebase codes. Production requires App Check. Protected workflows also require current verified email, a matching token/email, server-issued campus membership, protected role/access state and no deletion receipt. Claims, UID and totals supplied by clients do not establish authority. Activation and self-deletion accept pending identities under their separate guards; production deletion is approval-gated.

| API | Allowed roles | Main fields / behavior |
|---|---|---|
| `activateCampusAccess` | authenticated pending member | Empty object. Verifies exact institutional domain or approved email exception and both current Auth/token verification; grants membership without changing role. |
| `lookupAdmissionCandidate` | admin | Registered email. Returns a bounded account summary for institutional review. |
| `setAdmissionException` | admin | userId, approve boolean, approvalReference. Email-bound exception and audit; clears membership until fresh activation. Does not verify email or change role. |
| `eventRegistration` | student | eventId, cancel. Deterministic registration, transactional capacity/deadline/cancellation checks; returns registration and opaque ticket token. |
| `checkIn` | faculty/admin | registrationId, token. Organizer ownership, attendance time window, cancellation and single-use validation. |
| `publishCampusContent` | faculty/admin | kind, id, title/body, category/priority, department, venue, startTime/endTime, capacity, optional registrationDeadline/expiresAt, imagePath, clubId. Epoch milliseconds from date pickers; faculty department comes from trusted profile. |
| `setContentStatus` | faculty/admin | kind, id, ARCHIVE/RESTORE/CANCEL. Author ownership and audit; cancellation is irreversible. |
| `placeFoodOrder` | student | canteenId, stable requestId, item IDs/quantities, optional expectedTotalPaise. Server calculates authoritative prices; deterministic order ID prevents duplication. |
| `updateFoodOrder` | assigned vendor/admin; owner student may cancel PLACED only | orderId, next status. Enforces state graph. Completion is fulfillment, not a payment receipt. |
| `setMenuAvailability` / `saveMenuItem` | assigned vendor/admin | Verified kitchen/menu ownership and validated item values; source functions define detailed fields. |
| `setCanteenOperations` | assigned vendor/admin | canteenId, open, paused, preparationMinutes. Estimate comes from kitchen; no invented promise. |
| `submitReport` | student | stable requestId, title, description, category, location, optional imagePath. Private owner-scoped attachment validation. |
| `updateReport` | admin | reportId, status, named-selector assignedTo UID internally, resolutionNote. Validates provisioned active maintenance assignee and status graph. |
| `updateAssignedWork` | assigned maintenance | reportId, status, note, optional evidencePath. Progress/accept/start/request-review; cannot approve final resolution. |
| `manageTrip` | assigned driver/admin | busId, active. Trip and assignment transaction. |
| `publishTripLocation` | assigned driver | busId, coordinates, accuracy, capturedAt, locationSource. Reject stale/out-of-range/inactive/unassigned writes; throttle and server freshness timestamps. |
| `reportTransportIssue` | assigned driver/admin | busId, stable requestId, DELAY/BREAKDOWN/DISRUPTION/CLEARED, delayMinutes, note. Route-targeted inbox delivery. |
| `manageCampusData` | admin | supported collection, optional id and typed values. Names shown in Android selectors; backend validates reference IDs. Timetables serialize conflicting edits across rooms/faculty/cohorts. |
| `invitePrivilegedUser` | admin | email, name, role, department/team. Trusted Auth claims/profile/directory and setup link; never grants or overwrites administrator through invitations. Auth/Firestore provisioning spans services and needs reconciliation on partial failure. |
| `manageMess` | admin | hall/period/entitlement kind and fields. Requires provisioned operator/student identity and meal window up to twelve hours. |
| `getMealPass` | entitled student | messId. Opaque purpose-bound five-minute pass, not a sign-in credential. |
| `checkInMeal` | assigned mess/admin | periodId, userId, token. Entitlement/window/assignment checks and deterministic duplicate prevention; consumes pass. |
| `getMessSummary` | assigned mess/admin | periodId. Server aggregate count. |
| `getOperationsSummary` | admin | Server counts for active transport, orders, reports and recent meals. |
| `requestAccountDeletion` | recently authenticated student | confirm=true. Rejects active food orders and staff self-service, creates restricted durable cleanup job. |

Firestore triggers create durable notifications for notices, event updates/cancellation, transport disruptions and timetable changes. Push is optional and separate from inbox persistence. `completeAccountDeletion` retries cleanup after the app closes; status is restricted to operators. Trigger retries must be monitored in production.

Prices use integer paise. Operational timestamps are epoch milliseconds at the boundary and Firestore Timestamps at rest. Timetable minutes are campus-local wall time; choosing an institution timezone remains necessary for devices travelling across timezones. IDs remain implementation details behind named selectors.

Error handling: unauthenticated → sign in/reauthenticate; permission-denied → changed role/ownership; failed-precondition → refresh business state or complete prerequisite; invalid-argument → correct fields; resource-exhausted → retry after the quota window. Retry a creation with the same request identity. Do not blindly queue capacity, order or attendance mutations offline.

See `FIRESTORE_SCHEMA.md`, `SECURITY.md`, `RELEASE_RUNBOOK.md` and test sources for constraints and outstanding gates. This contract documents implemented source; it does not prove live deployment.
