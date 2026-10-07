# Civora Firebase Setup & Configuration Guide

This document explains how to set up, configure, and deploy the Firebase backend infrastructure for Civora.

---

## 1. Prerequisites

- **Firebase CLI**: Install globally via `npm install -g firebase-tools`
- **Node.js**: v20.x or v22.x LTS
- **Google Cloud Platform (GCP)** account with billing enabled for Cloud Functions (Blaze plan)
- **Java / Android SDK**: Android Studio with SDK 36+

---

## 2. Firebase Project Initialization

1. Create a Firebase project at [Firebase Console](https://console.firebase.google.com/).
2. Enable the following Firebase services:
   - **Authentication**: Enable the `Email/Password` provider.
   - **Cloud Firestore**: Create in production mode.
   - **Cloud Functions**: Node.js runtime (Region: `us-central1`).
   - **Cloud Messaging (FCM)**: Enabled by default.
3. Register the Android App:
   - **Package Name**: `com.sachinkanna.civora`
   - Download the generated `google-services.json` and place it in the `app/` directory (`app/google-services.json`).

---

## 3. Cloud Functions & Rules Deployment

1. Install dependencies:
   ```bash
   cd functions
   npm ci
   ```
2. Login to Firebase:
   ```bash
   firebase login
   ```
3. Deploy Firestore Rules, Composite Indexes, and Cloud Functions:
   ```bash
   firebase deploy --only firestore:rules,firestore:indexes,functions
   ```

---

## 4. Bootstrapping the Initial Administrator

1. Register an initial account through the Civora Android app (public registration always creates a `student` profile).
2. Note the user's `UID` from the Firebase Authentication console.
3. Run the bootstrap script from the `functions/` folder with Google Cloud Admin credentials:
   ```bash
   node scripts/bootstrap-admin.js <ADMIN_UID>
   ```
4. Sign out and sign back in on the Android app to refresh token claims. The account will now have full `ADMIN` role privileges.

---

## 5. Local Emulator Testing

For local development and testing without a live Firebase project:

1. Start Firebase Emulators:
   ```bash
   npx firebase-tools emulators:start --project demo-civora --only auth,firestore,functions
   ```
2. Run unit & workflow emulator tests:
   ```bash
   npm --prefix functions test
   npx firebase-tools emulators:exec --project demo-civora --only auth,firestore,functions "npm --prefix functions test"
   ```
3. Run the Android app targeting emulator:
   ```bash
   ./gradlew assembleDebug -Pcivora.emulatorHost=10.0.2.2
   ```

---

## 6. Security Considerations & App Check

- **Custom Claims**: Role authorization (`admin`, `faculty`, `vendor`, `driver`) is enforced via server-issued Firebase Auth custom claims.
- **App Check**: For production deployments, register Android Play Integrity attestation in Firebase Console to protect callable Cloud Functions against unauthorized abuse.
