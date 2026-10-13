package com.sachinkanna.civora.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.sachinkanna.civora.data.model.UserProfile
import com.sachinkanna.civora.data.model.UserRole
import kotlinx.coroutines.tasks.await

class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    suspend fun register(name: String, email: String, password: String): UserProfile {
        com.sachinkanna.civora.data.model.validateStudentRegistration(name, email, password)
        val result = auth.createUserWithEmailAndPassword(com.sachinkanna.civora.data.model.CampusPolicy.canonicalEmail(email), password).await()
        val uid = result.user?.uid ?: error("Firebase did not return a user id")
        val profile =
            UserProfile(
                uid = uid,
                name = name.trim(),
                email = result.user?.email ?: email.trim(),
                role = UserRole.STUDENT,
            )
        try {
            firestore
                .collection("users")
                .document(uid)
                .set(profile.toMap(), SetOptions.merge())
                .await()
        } catch (error: Throwable) {
            auth.currentUser?.delete()?.await()
            throw error
        }
        return profile
    }

    suspend fun login(email: String, password: String): UserProfile {
        val result = auth.signInWithEmailAndPassword(email.trim(), password).await()
        return fetchProfile(result.user?.uid ?: error("Firebase did not return a user id"))
    }

    suspend fun fetchCurrentProfile(): UserProfile? = auth.currentUser?.let { fetchProfile(it.uid) }

    suspend fun fetchProfile(uid: String): UserProfile {
        var data =
            firestore.collection("users").document(uid).get().await().data
                ?: error("Civora profile is missing")
        var claims = auth.currentUser?.getIdToken(false)?.await()?.claims.orEmpty()
        if(data["role"]!=(claims["role"] as? String ?: "student"))claims=auth.currentUser?.getIdToken(true)?.await()?.claims.orEmpty()
        val verified=claims["email_verified"]==true
        val currentEmail=claims["email"] as? String ?: auth.currentUser?.email.orEmpty()
        if(verified&&(data["membershipGranted"]!=true||data["membershipEmail"]!=com.sachinkanna.civora.data.model.CampusPolicy.canonicalEmail(currentEmail))) {
            try {
                com.google.firebase.functions.FirebaseFunctions.getInstance().getHttpsCallable("activateCampusAccess").call(emptyMap<String,Any>()).await()
                data=firestore.collection("users").document(uid).get().await().data ?: error("Civora profile is missing")
            } catch(error: com.google.firebase.functions.FirebaseFunctionsException) {
                if(error.code!=com.google.firebase.functions.FirebaseFunctionsException.Code.PERMISSION_DENIED)throw error
            }
        }
        val role = UserRole.fromKey(claims["role"] as? String) ?: UserRole.STUDENT
        val name = data["name"] as? String ?: error("Civora profile is incomplete")
        val email = currentEmail.ifBlank {data["email"] as? String ?: error("Civora profile is incomplete")}
        return UserProfile(
            uid,
            name,
            email,
            role,
            data["department"] as? String ?: "",
            data["year"] as? String ?: "",
            data["profileImage"] as? String ?: "",
            (data["createdAt"] as? com.google.firebase.Timestamp)?.toDate()?.time ?: 0L,
            data["section"] as? String ?: "",
            data["routeId"] as? String ?: "",
            data["notificationsEnabled"] as? Boolean ?: true,
            data["boardingStop"] as? String ?: "",
            data["themePreference"] as? String ?: "SYSTEM",
            data["kitchenAlerts"] as? Boolean ?: true,
            verified,
            verified&&data["membershipGranted"]==true&&data["membershipEmail"]==com.sachinkanna.civora.data.model.CampusPolicy.canonicalEmail(currentEmail)&&com.sachinkanna.civora.data.model.CampusPolicy.eligible(currentEmail,verified,data["admissionException"]==true,data["admissionEmail"] as? String ?: ""),
        )
    }

    suspend fun sendVerificationEmail() {
        (auth.currentUser ?: error("Sign in again")).sendEmailVerification().await()
    }
    suspend fun refreshVerifiedProfile(): UserProfile? {
        val user=auth.currentUser ?: return null
        user.reload().await()
        auth.currentUser?.getIdToken(true)?.await()
        return fetchCurrentProfile()
    }

    fun logout() = auth.signOut()

    suspend fun unregisterDevice() {
        if(com.sachinkanna.civora.FirebaseEnvironment.isDemo)return
        val messaging=com.google.firebase.messaging.FirebaseMessaging.getInstance()
        messaging.isAutoInitEnabled=false
        val uid=auth.currentUser?.uid ?: run {messaging.deleteToken().await();return}
        val context=com.google.firebase.FirebaseApp.getInstance().applicationContext
        val preferences=context.getSharedPreferences("civora_push_devices",android.content.Context.MODE_PRIVATE)
        val deviceId=preferences.getString(uid,"").orEmpty()
        try {
            if(deviceId.isNotBlank())firestore.collection("users").document(uid).collection("devices").document(deviceId).delete().await()
        } finally {
            preferences.edit().remove(uid).apply()
            messaging.deleteToken().await()
        }
    }

    suspend fun deleteAccount(password: String) {
        val user = auth.currentUser ?: error("Sign in again")
        val email = user.email ?: error("Email authentication is required")
        user.reauthenticate(com.google.firebase.auth.EmailAuthProvider.getCredential(email, password)).await()
        com.google.firebase.functions.FirebaseFunctions.getInstance()
            .getHttpsCallable("requestAccountDeletion").call(mapOf("confirm" to true)).await()
        val context = com.google.firebase.FirebaseApp.getInstance().applicationContext
        DeviceCartPersistence(context).clear(user.uid)
        context.getSharedPreferences("civora_push_devices",android.content.Context.MODE_PRIVATE).edit().remove(user.uid).apply()
        if(!com.sachinkanna.civora.FirebaseEnvironment.isDemo)runCatching {
            val messaging=com.google.firebase.messaging.FirebaseMessaging.getInstance()
            messaging.isAutoInitEnabled=false
            kotlinx.coroutines.withTimeout(3000){messaging.deleteToken().await()}
        }
        auth.signOut()
    }

    private fun UserProfile.toMap() =
        mapOf(
            "uid" to uid,
            "name" to name,
            "email" to email,
            "role" to role.roleKey,
            "section" to section,
            "routeId" to routeId,
            "notificationsEnabled" to notificationsEnabled,
            "department" to department,
            "year" to year,
            "profileImage" to profileImage,
            "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
        )
}
