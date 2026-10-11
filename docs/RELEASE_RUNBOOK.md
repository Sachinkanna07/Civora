# Civora release and rollback runbook

Branch: `feat/civora-production-completion`. Target: API 36, package `com.sachinkanna.civora`. Release preparation is separate from production authorization and Play publication.

KPRIET domain/timezone policy is in `functions/campus-config.json`, which generates Android constants. Complete email verification and membership activation for pilot accounts; reviewed exceptions are separate from privileged role provisioning. Supply authorized Civora contacts and policy/deletion URLs. Retention is proposed only; production account deletion remains disabled until both reviewed approval and activation flags are explicitly enabled. No institution endorsement is assumed. See `INSTITUTION_CONFIGURATION.md` before any production migration or deployment.

## Local verification

Use Java 21, Android SDK 36.1 and Node 22. Run sequential Android builds against stable source:

```powershell
.\gradlew.bat test assembleDebug lintDebug assembleRelease bundleRelease --max-workers=2
npm --prefix functions ci
npm --prefix functions audit --omit=dev --audit-level=high
$env:FUNCTIONS_DISCOVERY_TIMEOUT='120'
$env:CIVORA_REQUIRE_EMULATORS='true'
npx -y firebase-tools@latest emulators:exec --project demo-civora --only 'auth,firestore,functions,storage' 'npm --prefix functions test'
```

Do not run rule tests against a live project or concurrently with demo capture: those tests intentionally clear their disposable Firestore database. Tests reject non-loopback emulator endpoints and non-demo project IDs. The local stress scale is fifty orders at five concurrent requests per batch; it is not a fifty-concurrent production capacity certificate.

## Signing

Supply `CIVORA_KEYSTORE_PATH`, `CIVORA_KEYSTORE_PASSWORD`, `CIVORA_KEY_ALIAS` and `CIVORA_KEY_PASSWORD` through the operator's secure environment. All four are required to apply production signing. Do not put them in commands, repository files, logs or workflow YAML. This workflow does not create a signing key. Without those values Gradle may produce unsigned APK/AAB artifacts for inspection, which are not upload-ready signed releases.

Release enables R8/resource optimization and strips verbose/debug logs. HTTPS-only traffic applies to release; local debug uses its explicit emulator override. Verify the merged release manifest, signature, mapping and AAB package/version before approving upload. Ensure increasing versionCode for each subsequent Play release.

## Crashlytics and attestation

Android installs Play Integrity App Check for configured live apps; emulators bypass it. Production callable functions require App Check. Register the release app's signing fingerprints and verify attestation metrics before enabling client-facing access. Direct Firestore/Storage App Check enforcement is an external project setting and must also be reviewed.

Crashlytics SDK is present with collection disabled by default and an opt-in profile setting. The mapping/build-ID plugin is always applied because SDK initialization requires its generated build ID even with collection disabled. Mapping upload requires `'-Pcivora.uploadCrashlyticsMapping=true'`; use only after production authorization. Test receipt of a deliberate test crash in a non-public build and remove the temporary crash trigger before distribution. No live receipt is claimed from local compilation.

## Production gate

1. Approve the production deployment, Blaze billing, Firestore/Storage regions and budget ownership explicitly. Read-only checks currently report disabled Firestore. No region or billing account is guessed.
2. Initialize Email/Password Authentication, Firestore and Storage with reviewed production configuration. Verify local ignored `google-services.json` matches project/package. Keep credentials out of Git.
3. Back up existing data and review additive schema/migration. Never promote legacy self-selected profile roles into claims.
4. Review IAM and provision the first confirmed administrator with the trusted bootstrap script. Refresh tokens and validate role routing.
5. Deploy only to the named project after approval:

```powershell
npx -y firebase-tools@latest deploy --project civora-8f56e --only 'firestore,storage,functions'
```

6. Wait for all composite indexes, verify deployed function exports, App Check, negative permissions, role journeys, real FCM and moving-device GPS. Emulators do not enforce all production indexes.
7. Confirm budget alerts, logging access, deletion retry monitoring and agreed retention jobs. `requestLimits.expiresAt` is a TTL preparation field, not an activated TTL policy.
8. Complete public privacy/deletion URLs, Data Safety, foreground location declaration, institution contacts, signed bundle and representative real screenshots. Release only after the tracker gates pass.

## Recovery and rollback

Record deployment commit, regions, function revisions, rule/index versions and release mapping. For a faulty release, halt rollout in Play Console and restore a known tested source revision in a separate worktree. Run its tests before a reviewed redeployment to the explicit project. Do not force-push, reset unrelated workspace changes or blindly roll back a migrated schema. Firestore indexes are additive; avoid deleting required indexes during rollback.

For failed orders, inspect deterministic order ID, current status and callable error code. Retry the original request identity; never create a second order to mask an ambiguous response. For registration failures inspect capacity/deadline/cancellation and transaction logs. For GPS inspect consent, active assignment, foreground notification, timestamps and backoff. For push inspect profile preference, permission, device tokens and trigger logs without logging tokens. For media inspect ownership, JPEG validation and Storage rules. For deletion inspect `accountDeletions` status and Functions errors; retry the idempotent worker in a trusted maintenance operation rather than deleting the request receipt.

Staff deletion, institutional admission policy, comprehensive retention and live monitoring verification remain release blockers. This document is a runbook, not evidence those tasks passed.

## Immediate access suspension

With trusted ADC and the explicitly approved `GCLOUD_PROJECT`, run `node functions/scripts/set-account-access.js CONFIRMED_UID disable`. The tool blocks the protected profile first, then disables Auth and revokes refresh tokens. Existing privileged tokens also require a matching server-controlled profile role in database/media rules. Re-enable with `enable` only after institutional approval; deletion receipts and role mismatches prevent reactivation. Do not rely on an Auth-console-only disable to immediately invalidate cached tokens for direct reads. This operator tool has not been run against production.
