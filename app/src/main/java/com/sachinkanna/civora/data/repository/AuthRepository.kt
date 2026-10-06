package com.sachinkanna.civora.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.sachinkanna.civora.data.model.UserProfile
import com.sachinkanna.civora.data.model.UserRole
import kotlinx.coroutines.tasks.await

class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    suspend fun register(name: String, email: String, password: String, role: UserRole): UserProfile {
        val result = auth.createUserWithEmailAndPassword(email.trim(), password).await()
        val uid = result.user?.uid ?: error("Firebase did not return a user id")
        val profile = UserProfile(uid = uid, name = name.trim(), email = email.trim(), role = role)
        try { firestore.collection("users").document(uid).set(profile.toMap(), SetOptions.merge()).await() }
        catch (error: Throwable) { auth.currentUser?.delete()?.await(); throw error }
        return profile
    }

    suspend fun login(email: String, password: String): UserProfile {
        val result = auth.signInWithEmailAndPassword(email.trim(), password).await()
        return fetchProfile(result.user?.uid ?: error("Firebase did not return a user id"))
    }

    suspend fun fetchCurrentProfile(): UserProfile? = auth.currentUser?.let { fetchProfile(it.uid) }

    suspend fun fetchProfile(uid: String): UserProfile {
        val data = firestore.collection("users").document(uid).get().await().data
            ?: error("Civora profile is missing")
        val role = UserRole.fromKey(data["role"] as? String) ?: error("Civora profile has an invalid role")
        val name = data["name"] as? String ?: error("Civora profile is incomplete")
        val email = data["email"] as? String ?: error("Civora profile is incomplete")
        return UserProfile(uid, name, email, role,
            data["department"] as? String ?: "", data["year"] as? String ?: "", data["profileImage"] as? String ?: "",
            (data["createdAt"] as? Number)?.toLong() ?: 0L)
    }

    fun logout() = auth.signOut()

    private fun UserProfile.toMap() = mapOf("uid" to uid, "name" to name, "email" to email, "role" to role.roleKey,
        "department" to department, "year" to year, "profileImage" to profileImage, "createdAt" to createdAt)
}
