package com.sachinkanna.civora.data.datasource

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Looper
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import com.google.firebase.firestore.*

/** Foreground-only location publisher. Never requests background permission. */
class DriverLocationSource(
    context: Context,
    private val tripId: String,
    private val onState: (String) -> Unit,
) {
    private val context = context.applicationContext
    private val debug = (context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
    private val client = LocationServices.getFusedLocationProviderClient(context)
    private var lastSent = 0L
    private var inFlight = false
    private var running = false
    private val callback =
        object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location = result.lastLocation ?: return
                val now = android.os.SystemClock.elapsedRealtime()
                if (
                    !running ||
                        inFlight ||
                        now - lastSent < 15_000 ||
                        location.accuracy > 200 ||
                        (androidx.core.location.LocationCompat.isMock(location) && !debug)
                )
                    return
                lastSent = now
                inFlight = true
                FirebaseFirestore.getInstance()
                    .collection("activeTrips")
                    .document(tripId)
                    .update(
                        mapOf(
                            "latitude" to location.latitude,
                            "longitude" to location.longitude,
                            "accuracy" to location.accuracy.toDouble(),
                            "locationCapturedAt" to
                                com.google.firebase.Timestamp(java.util.Date(location.time)),
                            "locationUpdatedAt" to FieldValue.serverTimestamp(),
                        )
                    )
                    .addOnSuccessListener { onState("Location shared") }
                    .addOnFailureListener {
                        onState("Location could not be shared. Check your connection.")
                    }
                    .addOnCompleteListener { inFlight = false }
            }
        }

    fun start() {
        if (
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            ) != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                ) != PackageManager.PERMISSION_GRANTED
        ) {
            onState("Location permission needed")
            return
        }
        running = true
        onState("Waiting for GPS. Keep this screen open.")
        try {
            client
                .requestLocationUpdates(
                    LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, 15_000)
                        .setMinUpdateIntervalMillis(15_000)
                        .build(),
                    callback,
                    Looper.getMainLooper(),
                )
                .addOnFailureListener { onState("Location service unavailable") }
        } catch (e: SecurityException) {
            onState("Location permission denied")
        }
    }

    fun stop() {
        running = false
        client.removeLocationUpdates(callback)
    }
}
