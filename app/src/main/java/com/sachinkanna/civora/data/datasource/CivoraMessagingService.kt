package com.sachinkanna.civora.data.datasource

import android.Manifest
import android.app.*
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.*
import com.sachinkanna.civora.MainActivity

class CivoraMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        registerDevice(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        if (message.data["userId"] != uid) return
        FirebaseFirestore.getInstance()
            .collection("users")
            .document(uid)
            .get()
            .addOnSuccessListener { profile ->
                if (FirebaseAuth.getInstance().currentUser?.uid != uid) return@addOnSuccessListener
                if (profile.getBoolean("notificationsEnabled") == false) return@addOnSuccessListener
                if (
                    Build.VERSION.SDK_INT >= 33 &&
                        ContextCompat.checkSelfPermission(
                            this,
                            Manifest.permission.POST_NOTIFICATIONS,
                        ) != PackageManager.PERMISSION_GRANTED
                )
                    return@addOnSuccessListener
                val manager = getSystemService(NotificationManager::class.java)
                manager.createNotificationChannel(
                    NotificationChannel(
                        "campus",
                        "Campus updates",
                        NotificationManager.IMPORTANCE_DEFAULT,
                    )
                )
                val intent =
                    PendingIntent.getActivity(
                        this,
                        0,
                        Intent(this, MainActivity::class.java),
                        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                    )
                val notification =
                    Notification.Builder(this, "campus")
                        .setSmallIcon(com.sachinkanna.civora.R.drawable.ic_launcher_foreground)
                        .setContentTitle(message.data["title"] ?: "Civora update")
                        .setContentText(message.data["body"] ?: "Open your campus inbox.")
                        .setContentIntent(intent)
                        .setAutoCancel(true)
                        .build()
                manager.notify(
                    (message.data["notificationId"] ?: message.messageId ?: "campus").hashCode(),
                    notification,
                )
            }
    }

    companion object {
        fun registerDevice(token: String) {
            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
            val deviceId =
                java.security.MessageDigest.getInstance("SHA-256")
                    .digest(token.toByteArray())
                    .joinToString("") { "%02x".format(it) }
            FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .collection("devices")
                .document(deviceId)
                .set(
                    mapOf(
                        "token" to token,
                        "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                    )
                )
        }
    }
}
